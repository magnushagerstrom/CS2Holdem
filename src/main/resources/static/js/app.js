/*
 * CS2Holdem web front end: one page, plain JavaScript, no build step.
 *
 * The page asks the Spring Boot API for data with fetch() and draws one "window" at a time.
 * Which window is open is decided by the part of the address after #, for example
 * cs2holdem.com/#/hand. Changing it does not reload the page, so the login and data stay.
 */
'use strict';

const state = {
  me: null,       // { steamId64, registered } when signed in, otherwise null
  selected: [],   // card ids picked while building a hand
};

// ---------- Talking to the API ----------

/** Calls the API and returns the JSON answer. Throws an Error with the server's explanation. */
async function api(path, options = {}) {
  const response = await fetch(path, {
    method: options.method || 'GET',
    credentials: 'same-origin', // send the login cookie
    headers: options.body ? { 'Content-Type': 'application/json' } : {},
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    // Our errors follow RFC 9457 (Problem Details): title, detail and per-field errors
    const error = new Error((data && (data.detail || data.title)) || `The server answered ${response.status}`);
    error.status = response.status;
    error.fields = (data && data.errors) || {};
    throw error;
  }
  return data;
}

async function loadMe() {
  try {
    state.me = await api('/api/me');
  } catch (e) {
    state.me = null; // 401: not signed in
  }
}

// ---------- Small helpers ----------

/** Makes text safe to put inside HTML, so a player name can never inject code. */
function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

const FACES = { SPECIAL_KILL: '1K', DOUBLE_KILL: '2K', TRIPLE_KILL: '3K', QUAD_KILL: '4K', ACE: 'ACE' };

const STATUS = {
  DISCOVERED: 'Waiting for the demo',
  DEMO_LOCATED: 'Demo found, reading soon',
  DOWNLOADED: 'Uploaded, reading soon',
  PARSED: 'Cards ready',
  EXPIRED: 'Demo deleted by Valve',
  FAILED: 'Could not be read',
};

function date(iso) {
  return iso ? new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : '';
}

/** A player's Steam avatar, or nothing if we have not fetched it yet. */
function avatar(url) {
  return url ? `<img class="avatar" src="${esc(url)}" alt="" width="20" height="20" loading="lazy">` : '';
}

function stamps(list) {
  if (!list || list.length === 0) return '';
  return `<div class="stamps">${list.map(s => `<span class="stamp">${esc(s)}</span>`).join('')}</div>`;
}

/** One card. As a button when it can be picked for a hand. */
function cardHtml(card, pickable = false) {
  const tag = pickable ? 'button' : 'div';
  const pressed = pickable ? ` type="button" aria-pressed="${state.selected.includes(card.id)}" data-card="${esc(card.id)}"` : '';
  const killStamps = card.kills.flatMap(k => k.stamps);
  return `<${tag} class="card"${pressed}>
    <span class="card-points">${card.points}</span>
    <span class="card-face">${FACES[card.type] || '?'}</span>
    <span class="card-name">${esc(card.typeName)}</span>
    ${stamps([...new Set([...card.stamps, ...killStamps])])}
    <span class="card-meta">Round ${card.round}<br>${card.totalDamage} damage</span>
  </${tag}>`;
}

/** An ordinary kill in a straight or flush hand. */
function killHtml(kill) {
  return `<div class="card">
    <span class="card-points">${kill.points}</span>
    <span class="card-face">1</span>
    <span class="card-name">${esc(kill.weaponGroup)}</span>
    ${stamps(kill.stamps)}
    <span class="card-meta">${esc(kill.mapArea || '')}<br>Round ${kill.round ?? ''}, ${kill.damage} damage</span>
  </div>`;
}

// ---------- Drawing ----------

function drawMenu(route) {
  const items = state.me
    ? [['#/hand', 'My hand'], ['#/collection', 'Collection'], ['#/matches', 'Matches'],
       ['#/leaderboard', 'Leaderboard'], ['#/how', 'How it works']]
    : [['/auth/steam', 'Sign in through Steam'], ['#/how', 'How it works'], ['#/leaderboard', 'Leaderboard']];

  const links = items.map(([href, text]) => {
    const current = href === '#' + route ? ' aria-current="page"' : '';
    return `<li><a href="${href}"${current}>${text}</a></li>`;
  });
  if (state.me) links.push('<li><button type="button" id="sign-out">Sign out</button></li>');
  document.getElementById('menu-items').innerHTML = links.join('');

  const player = document.getElementById('menu-player');
  player.hidden = !state.me;
  if (state.me) {
    // The Steam name arrives within a minute of the first login; until then the SteamID stands in
    player.innerHTML = `${avatar(state.me.avatarUrl)}<span>${esc(state.me.displayName || state.me.steamId64)}</span>`;
  }

  const signOut = document.getElementById('sign-out');
  if (signOut) signOut.addEventListener('click', async () => {
    await api('/auth/logout', { method: 'POST' }).catch(() => {});
    state.me = null;
    location.hash = '#/';
    showRoute();
  });
}

function drawWindow(title, body) {
  document.getElementById('window-area').innerHTML = `
    <section class="window" aria-labelledby="window-title">
      <div class="window-title">
        <span id="window-title">${esc(title)}</span>
        <a class="button window-close" href="#/" aria-label="Close">x</a>
      </div>
      <div class="window-body">${body}</div>
    </section>`;
}

function drawError(title, error) {
  drawWindow(title, `<p class="error">${esc(error.message)}</p>`);
}

// ---------- Windows ----------

function howItWorks() {
  const table = (head, rows) => `<div class="table-wrap"><table>
      <thead><tr>${head.map(h => `<th>${h}</th>`).join('')}</tr></thead>
      <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></div>`;

  drawWindow('How it works', `
    <p>Your best rounds in CS2 become playing cards. Over a season you collect them, and the
      strongest poker hand you can build from them is your season hand. When the season ends,
      the strongest hand wins.</p>

    <h2>What becomes a card</h2>
    <p>A card is one round where you did something worth showing off. Ordinary single kills do
      not make cards on their own.</p>
    ${table(['Card', 'In one round you got', 'Kills'], [
      ['<strong>1K</strong> Special kill', 'a kill with a knife or a Zeus, or a no-scope with any weapon that has a scope', '1'],
      ['<strong>2K</strong> Double kill', 'two kills', '2'],
      ['<strong>3K</strong> Triple kill', 'three kills', '3'],
      ['<strong>4K</strong> Quad kill', 'four kills', '4'],
      ['<strong>ACE</strong> Ace', 'the whole enemy team', '5'],
    ])}
    <p class="dim">The weapons with a scope are the AWP, SSG 08, SCAR-20, G3SG1, AUG and SG 553.
      A round gives at most one card. Five kills is an ace, not a triple kill and a double kill.</p>

    <h2>Which kills count</h2>
    <p>Only matches on Valve's matchmaking count: Premier, Competitive and Wingman. Within them,
      a kill counts when the enemy was a real player, the round was live, and you dealt the damage
      yourself. Team kills, bots, warmup and falling damage never count.</p>
    <p>Exit frags do not count either: kills after the round is decided, when the bomb has gone off,
      been defused or the last enemy is dead. Every kill is still saved, though, because straights and
      flushes are built from single kills.</p>

    <h2>Hands, weakest to strongest</h2>
    <p>A hand holds at most five kills, and its cards come from different rounds. They can come
      from any match in the season, except straights and flushes, which happen within one match.</p>
    ${table(['Hand', 'Built from', 'Base points'], [
      ['High card', 'one special kill', '10'],
      ['Pair', 'a double kill', '20'],
      ['Two pair', 'two double kills', '35'],
      ['Three of a kind', 'a triple kill', '50'],
      ['Straight', 'a kill in five rounds in a row, with five different weapon types', '70'],
      ['Flush', 'five kills in the same place on the map, within five rounds in a row', '90'],
      ['Full house', 'a triple kill and a double kill', '110'],
      ['Four of a kind', 'a quad kill', '140'],
      ['Straight flush', 'a straight where all five kills are in the same place', '170'],
      ['Five of a kind', 'an ace', '200'],
      ['Royal flush', 'a perfect ace: five headshots, or all 500 damage dealt by you', '250'],
    ])}
    <p class="dim">The weapon types for a straight are rifle, sniper, pistol, SMG, shotgun, heavy,
      grenade and knife or Zeus. An AK-47 and an M4 are the same type, so a straight means buying
      different weapons on purpose. The place for a flush is the callout name, such as Mid on Mirage.</p>

    <h2>Stamps</h2>
    <p>Stamps tell how a kill happened, and each one adds points. They never change the hand type,
      but they decide which of two full houses is stronger.</p>
    ${table(['Stamp', 'Given when', 'Points'], [
      ['Headshot', 'the kill was a headshot', '4'],
      ['Flashed', 'the enemy was blinded by a flashbang', '4'],
      ['Smoked', 'the enemy was killed through or inside a smoke', '8'],
      ['Burning', 'the enemy was standing in fire', '8'],
      ['Wallbang', 'the bullet went through a wall', '8'],
      ['Blind', 'you were blinded by a flashbang', '12'],
      ['On fire', 'you were standing in fire', '12'],
      ['Clutch', 'you were the last one alive on your team', '12'],
      ['Lightning', 'all kills of the card came within a few seconds', '12'],
      ['No-scope', 'a kill without zooming in, with a weapon that has a scope', '20'],
      ['One grenade', 'all kills of the card came from one HE grenade', '20'],
      ['One shot', 'all kills of the card came from a single bullet', '20'],
    ])}
    <p class="dim">Clutch, Lightning, One grenade and One shot belong to the whole card. All the
      others belong to single kills, so a card can earn them several times: a triple kill with three
      headshots gets the headshot points three times.</p>

    <h2>Points</h2>
    <p>Every hand gets one score: the base points of the hand type, plus points for every kill,
      plus every stamp. A kill is worth more the more damage it did and the harder the weapon is
      to hit with: a Desert Eagle or a scout counts for more than an AK, and a knife or Zeus
      counts the most. So a spectacular three of a kind can beat a dull full house.</p>

    <h2>Getting started</h2>
    <p>Sign in through Steam and connect your match history once. New matches are found
      automatically, and their cards appear some minutes after the match ends, depending on how
      fast Valve sends the replay.</p>
    ${state.me ? '' : '<div class="buttons"><a class="button" href="/auth/steam">Sign in through Steam</a></div>'}`);
}

function register() {
  drawWindow('Connect your match history', `
    <p>CS2Holdem reads your matches from Steam. Steam needs two codes to share them with us.
      You only do this once.</p>
    <form id="register-form" novalidate>
      <label for="authCode">Authentication code</label>
      <input type="text" id="authCode" name="authCode" placeholder="XXXX-XXXXX-XXXX" autocomplete="off" required>
      <p class="hint">Create it on <a href="https://help.steampowered.com/en/wizard/HelpWithGameIssue/?appid=730&issueid=128"
        target="_blank" rel="noopener">Steam's match history page</a>.</p>
      <p class="hint error" data-error="authCode"></p>

      <label for="knownShareCode">Share code of a recent match</label>
      <input type="text" id="knownShareCode" name="knownShareCode" placeholder="CSGO-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx" autocomplete="off" required>
      <p class="hint">In CS2: Watch, Your Matches, then copy the share code of any match.</p>
      <p class="hint error" data-error="knownShareCode"></p>

      <p class="error" id="register-error"></p>
      <div class="buttons"><button class="button" type="submit">Connect match history</button></div>
    </form>`);

  document.getElementById('register-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.target;
    const button = form.querySelector('button');
    form.querySelectorAll('[data-error]').forEach(p => (p.textContent = ''));
    document.getElementById('register-error').textContent = '';
    button.disabled = true;
    button.textContent = 'Connecting...';
    try {
      const result = await api('/api/accounts', {
        method: 'POST',
        body: {
          steamId64: state.me.steamId64,
          authCode: form.authCode.value.trim(),
          knownShareCode: form.knownShareCode.value.trim(),
        },
      });
      state.me.registered = true;
      location.hash = '#/matches';
    } catch (e) {
      for (const [field, message] of Object.entries(e.fields)) {
        const p = form.querySelector(`[data-error="${field}"]`);
        if (p) p.textContent = message;
      }
      if (Object.keys(e.fields).length === 0) document.getElementById('register-error').textContent = e.message;
      button.disabled = false;
      button.textContent = 'Connect match history';
    }
  });
}

async function matches() {
  drawWindow('Matches', '<p class="dim">Loading matches...</p>');
  try {
    const list = await api(`/api/accounts/${state.me.steamId64}/matches`);
    const rows = list.map(m => `<tr>
        <td>${date(m.discoveredAt)}</td>
        <td>${esc(m.mapName || '')}</td>
        <td>${esc(STATUS[m.status] || m.status)}</td>
      </tr>`).join('');
    drawWindow('Matches', `
      <p>${list.length} matches in your history. Cards appear when a match says <em>Cards ready</em>.</p>
      <div class="table-wrap"><table>
        <thead><tr><th>Found</th><th>Map</th><th>Status</th></tr></thead>
        <tbody>${rows}</tbody>
      </table></div>
      <p class="dim" id="sync-result"></p>
      <div class="buttons"><button class="button" type="button" id="sync">Check for new matches</button></div>`);

    document.getElementById('sync').addEventListener('click', async event => {
      event.target.disabled = true;
      event.target.textContent = 'Checking...';
      try {
        const result = await api(`/api/accounts/${state.me.steamId64}/sync`, { method: 'POST' });
        if (result.newMatches > 0) return matches();
        document.getElementById('sync-result').textContent = 'No new matches since the last check.';
      } catch (e) {
        document.getElementById('sync-result').textContent = e.message;
      }
      event.target.disabled = false;
      event.target.textContent = 'Check for new matches';
    });
  } catch (e) {
    drawError('Matches', e);
  }
}

async function collection() {
  drawWindow('Collection', '<p class="dim">Loading cards...</p>');
  try {
    const data = await api(`/api/players/${state.me.steamId64}/collection`);
    const cards = [...data.cards].sort((a, b) => b.points - a.points);
    drawWindow(`Collection, ${data.season.name}`, cards.length === 0
      ? '<p>No cards yet this season. Play a match, and its cards appear here a few minutes after it is read.</p>'
      : `<p>${cards.length} cards and ${data.kills.length} kills this season, strongest first.</p>
         <div class="cards">${cards.map(c => cardHtml(c)).join('')}</div>`);
  } catch (e) {
    drawError('Collection', e);
  }
}

async function hand() {
  drawWindow('My hand', '<p class="dim">Loading your hand...</p>');
  try {
    const current = await api(`/api/players/${state.me.steamId64}/hand`);
    if (!current) {
      drawWindow('My hand', `<p>You have no hand yet this season. It is built automatically from your
        cards as soon as you have one.</p><div class="buttons"><a class="button" href="#/matches">See matches</a></div>`);
      return;
    }
    const items = current.items.map(i => (i.card ? cardHtml(i.card) : killHtml(i.kill))).join('');
    drawWindow(`My hand, ${current.season.name}`, `
      <div class="hand-head">
        <span class="hand-type">${esc(current.typeName)}</span>
        <span class="hand-score">${current.score} points</span>
        <span class="dim">${current.totalDamage} damage${current.custom ? ', built by you' : ', the strongest you can make'}</span>
      </div>
      <div class="cards">${items}</div>
      <div class="buttons">
        <a class="button" href="#/build">Build my own hand</a>
        ${current.custom ? '<button class="button" type="button" id="reset">Use the strongest hand</button>' : ''}
      </div>`);

    const reset = document.getElementById('reset');
    if (reset) reset.addEventListener('click', async () => {
      await api(`/api/players/${state.me.steamId64}/hand/custom`, { method: 'DELETE' });
      hand();
    });
  } catch (e) {
    drawError('My hand', e);
  }
}

async function build() {
  drawWindow('Build a hand', '<p class="dim">Loading cards...</p>');
  try {
    const data = await api(`/api/players/${state.me.steamId64}/collection`);
    const cards = [...data.cards].sort((a, b) => b.points - a.points);
    const killsIn = ids => cards.filter(c => ids.includes(c.id)).reduce((n, c) => n + c.kills.length, 0);
    state.selected = [];

    const redraw = () => {
      const kills = killsIn(state.selected);
      drawWindow('Build a hand', `
        <p>Pick cards worth at most five kills together. Two cards from the same round cannot be in one hand.</p>
        <p><strong>${kills} of 5 kills</strong> picked.</p>
        <div class="cards">${cards.map(c => cardHtml(c, true)).join('')}</div>
        <p class="error" id="build-error"></p>
        <div class="buttons">
          <button class="button" type="button" id="save" ${state.selected.length === 0 ? 'disabled' : ''}>Save hand</button>
          <a class="button" href="#/hand">Cancel</a>
        </div>`);

      document.querySelectorAll('[data-card]').forEach(button => button.addEventListener('click', () => {
        const id = button.dataset.card;
        const next = state.selected.includes(id) ? state.selected.filter(x => x !== id) : [...state.selected, id];
        if (killsIn(next) > 5) {
          document.getElementById('build-error').textContent = 'That would be more than five kills.';
          return;
        }
        state.selected = next;
        redraw();
      }));

      document.getElementById('save').addEventListener('click', async () => {
        try {
          await api(`/api/players/${state.me.steamId64}/hand`, { method: 'PUT', body: { cardIds: state.selected } });
          location.hash = '#/hand';
        } catch (e) {
          document.getElementById('build-error').textContent = e.message;
        }
      });
    };
    redraw();
  } catch (e) {
    drawError('Build a hand', e);
  }
}

async function leaderboard() {
  drawWindow('Leaderboard', '<p class="dim">Loading the leaderboard...</p>');
  try {
    const [season, entries] = await Promise.all([
      api('/api/seasons/current'),
      api('/api/seasons/current/leaderboard'),
    ]);
    const rows = entries.map(e => `<tr>
        <td class="num">${e.rank}</td>
        <td class="player">${avatar(e.avatarUrl)}${esc(e.displayName || e.steamId64)}</td>
        <td>${esc(e.typeName)}</td>
        <td class="num">${e.score}</td>
      </tr>`).join('');
    drawWindow(`Leaderboard, ${season.name}`, entries.length === 0
      ? '<p>No hands yet this season.</p>'
      : `<div class="table-wrap"><table>
          <thead><tr><th class="num">#</th><th>Player</th><th>Hand</th><th class="num">Points</th></tr></thead>
          <tbody>${rows}</tbody>
        </table></div>`);
  } catch (e) {
    drawError('Leaderboard', e);
  }
}

// ---------- Which window to open ----------

const ROUTES = {
  '/how': howItWorks,
  '/leaderboard': leaderboard,
  '/register': register,
  '/matches': matches,
  '/collection': collection,
  '/hand': hand,
  '/build': build,
};
const NEEDS_LOGIN = ['/register', '/matches', '/collection', '/hand', '/build'];

function showRoute() {
  let route = location.hash.replace(/^#/, '') || '/';

  // Where "home" leads depends on who is looking
  if (route === '/') {
    route = !state.me ? '/how' : state.me.registered ? '/hand' : '/register';
  }
  if (NEEDS_LOGIN.includes(route) && !state.me) route = '/how';
  if (state.me && !state.me.registered && ['/matches', '/collection', '/hand', '/build'].includes(route)) {
    route = '/register';
  }

  drawMenu(route);
  (ROUTES[route] || howItWorks)();
}

window.addEventListener('hashchange', showRoute);
loadMe().then(showRoute);
