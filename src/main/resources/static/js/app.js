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

/*
 * The stamps, with an icon each. Icons are drawn on a 16 x 16 grid with lines in the current
 * text colour, so they follow the theme. Keys are the names the API sends.
 */
const STAMPS = {
  'Headshot': { points: 4, when: 'the kill was a headshot',
    // A head and shoulders with the bullet's path straight through the head
    icon: '<circle cx="8" cy="6.3" r="4"/><path d="M2.5 15c0-3 2.5-4.8 5.5-4.8s5.5 1.8 5.5 4.8M.8 5.6h14.4M13 3.9l2.2 1.7L13 7.3"/>' },
  'Flashed': { points: 4, when: 'the enemy was blinded by a flashbang',
    icon: '<circle cx="8" cy="8" r="2" fill="currentColor"/><path d="M8 1v3M8 12v3M1 8h3M12 8h3M3 3l2 2M11 11l2 2M13 3l-2 2M5 11l-2 2"/>' },
  'Smoked': { points: 8, when: 'the enemy was killed through or inside a smoke',
    icon: '<path d="M4.5 12.5h7a3 3 0 0 0 .3-6 4 4 0 0 0-7.6-.8 2.9 2.9 0 0 0 .3 6.8z"/>' },
  'Burning': { points: 8, when: 'the enemy was standing in fire',
    icon: '<path d="M8 15c2.8 0 4.5-1.9 4.5-4.5 0-3-2.8-4-2.8-8-2 1-4 3.2-4 6-.9-.6-1.2-1.6-1.2-2.6C3 7.4 3.5 9 3.5 10.5 3.5 13.1 5.2 15 8 15z"/>' },
  'Wallbang': { points: 8, when: 'the bullet went through a wall',
    icon: '<path d="M6 2h4v12H6zM6 6h4M6 10h4M8 2v4M8 10v4"/><path d="M1 8h14M12 5.5 14.5 8 12 10.5"/>' },
  'Blind': { points: 12, when: 'you were blinded by a flashbang',
    icon: '<path d="M1.5 8S4 4 8 4s6.5 4 6.5 4S12 12 8 12 1.5 8 1.5 8z"/><circle cx="8" cy="8" r="1.8"/><path d="M2.5 13.5 13.5 2.5"/>' },
  'On fire': { points: 12, when: 'you were standing in fire',
    icon: '<path fill="currentColor" d="M8 15c2.8 0 4.5-1.9 4.5-4.5 0-3-2.8-4-2.8-8-2 1-4 3.2-4 6-.9-.6-1.2-1.6-1.2-2.6C3 7.4 3.5 9 3.5 10.5 3.5 13.1 5.2 15 8 15z"/>' },
  'Clutch': { points: 12, when: 'you were the last one alive on your team',
    icon: '<circle cx="4" cy="8" r="2.6" fill="currentColor"/><circle cx="11" cy="3.5" r="1.7"/><circle cx="13.5" cy="8" r="1.7"/><circle cx="11" cy="12.5" r="1.7"/>' },
  'Lightning': { points: 12, when: 'all kills of the card came within a few seconds',
    icon: '<path d="M9.5 1 3.5 9h4l-1 6 6-8h-4z"/>' },
  'No-scope': { points: 20, when: 'a kill without zooming in, with a weapon that has a scope',
    icon: '<circle cx="8" cy="8" r="6"/><path d="M8 4v8M4 8h8M2.5 13.5 13.5 2.5"/>' },
  'One grenade': { points: 20, when: 'all kills of the card came from one HE grenade',
    icon: '<circle cx="7.5" cy="10" r="4.5"/><path d="M5.5 5.5V4h4v1.5"/><circle cx="12" cy="3" r="1.5"/>' },
  'One shot': { points: 20, when: 'all kills of the card came from a single bullet',
    icon: '<circle cx="6" cy="8" r="2.4"/><circle cx="11" cy="8" r="2.4"/><path d="M1 8h14"/>' },
};

function stampIcon(name) {
  const stamp = STAMPS[name];
  if (!stamp) return '';
  return `<svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" fill="none" stroke="currentColor"
    stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">${stamp.icon}</svg>`;
}

/**
 * The stamps as icons. A stamp earned several times (three headshots) shows once with a count.
 * The name and meaning are in the tooltip and for screen readers; unknown stamps fall back to text.
 */
function stamps(list) {
  if (!list || list.length === 0) return '';
  const counts = new Map();
  list.forEach(name => counts.set(name, (counts.get(name) || 0) + 1));
  return `<div class="stamps">${[...counts].map(([name, count]) => {
    const stamp = STAMPS[name];
    if (!stamp) return `<span class="stamp">${esc(name)}</span>`;
    const label = `${name}: ${stamp.when}, +${stamp.points * count} points`;
    return `<span class="stamp-icon" title="${esc(label)}" role="img" aria-label="${esc(label)}">${stampIcon(name)}${
      count > 1 ? `<span class="stamp-count">${count}</span>` : ''}</span>`;
  }).join('')}</div>`;
}

/** The card's rarity, in CS2's skin colours. A perfect ace (royal flush) is gold. */
function rarity(card) {
  if (card.type !== 'ACE') return card.type;
  const perfect = card.totalDamage >= 500 || card.kills.every(k => k.stamps.includes('Headshot'));
  return perfect ? 'GOLD' : 'ACE';
}

/** One card. As a button when it can be picked for a hand. */
function cardHtml(card, pickable = false) {
  const tag = pickable ? 'button' : 'div';
  const pressed = pickable ? ` type="button" aria-pressed="${state.selected.includes(card.id)}" data-card="${esc(card.id)}"` : '';
  const killStamps = card.kills.flatMap(k => k.stamps);
  return `<${tag} class="card" data-rarity="${rarity(card)}"${pressed}>
    <span class="card-points">${card.points}</span>
    <span class="card-face">${FACES[card.type] || '?'}</span>
    <span class="card-name">${esc(card.typeName)}</span>
    ${stamps([...card.stamps, ...killStamps])}
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
    ? [['#/hand', 'My hand'], ['#/player/' + state.me.steamId64, 'My profile'], ['#/collection', 'Collection'], ['#/matches', 'Matches'],
       ['#/leaderboard', 'Leaderboard'], ['#/how', 'How it works'], ['#/privacy', 'Privacy']]
    : [['/auth/steam', 'Sign in through Steam'], ['#/how', 'How it works'], ['#/leaderboard', 'Leaderboard'],
       ['#/privacy', 'Privacy']];

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
      ['<strong class="rarity" data-rarity="SPECIAL_KILL">1K</strong> Special kill', 'a kill with a knife or a Zeus, or a no-scope with any weapon that has a scope', '1'],
      ['<strong class="rarity" data-rarity="DOUBLE_KILL">2K</strong> Double kill', 'two kills', '2'],
      ['<strong class="rarity" data-rarity="TRIPLE_KILL">3K</strong> Triple kill', 'three kills', '3'],
      ['<strong class="rarity" data-rarity="QUAD_KILL">4K</strong> Quad kill', 'four kills', '4'],
      ['<strong class="rarity" data-rarity="ACE">ACE</strong> Ace', 'the whole enemy team', '5'],
      ['<strong class="rarity" data-rarity="GOLD">ACE</strong> Perfect ace', 'the whole enemy team with five headshots, or all 500 damage yourself', '5'],
    ])}
    <p class="dim">The weapons with a scope are the AWP, SSG 08, SCAR-20, G3SG1, AUG and SG 553.
      A round gives at most one card. Five kills is an ace, not a triple kill and a double kill.
      The colour shows how rare a card is, the same way skin rarity does in CS2.</p>

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
    ${table(['Stamp', 'Given when', 'Points'], Object.entries(STAMPS).map(([name, stamp]) =>
      [`<span class="stamp-name">${stampIcon(name)}${esc(name)}</span>`, stamp.when, String(stamp.points)]))}
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

/** Where Steam's sign-in ends up when it did not go through (see AuthController). */
function signInCancelled() {
  drawWindow('Sign-in cancelled', `
    <p>You left Steam's sign-in page before signing in, so nothing has changed.</p>
    <div class="buttons"><a class="button" href="/auth/steam">Sign in through Steam</a>
      <a class="button" href="#/how">How it works</a></div>`);
}

function signInFailed() {
  drawWindow('Sign-in did not go through', `
    <p>Steam could not confirm the sign-in. This usually means Steam is busy or down for a moment.
      Wait a minute and try again.</p>
    <p class="dim">If it keeps happening, check that Steam's own site works:
      <a href="https://steamcommunity.com" target="_blank" rel="noopener">steamcommunity.com</a>.</p>
    <div class="buttons"><a class="button" href="/auth/steam">Try again</a></div>`);
}

function privacy() {
  const signedIn = Boolean(state.me);
  drawWindow('Privacy', `
    <p>CS2Holdem reads the demos of matches its players have played, and turns the best rounds into
      cards. This is what that means for your data.</p>

    <h2>What we store</h2>
    <ul>
      <li>Your SteamID, Steam name and avatar.</li>
      <li>For every match we read: your kills, cards and scoreboard numbers (kills, deaths, damage and so on).</li>
      <li>If you registered: the match history code you gave us, encrypted, so we can find your new matches.</li>
    </ul>
    <p>We do not store your Steam password, your email, chat messages or anything outside the matches.</p>

    <h2>Where it comes from</h2>
    <p>Names, avatars and lifetime statistics come from Steam's public profile. Everything else
      comes from the match demos that Valve publishes for every matchmaking game.</p>

    <h2>Who can see it</h2>
    <p>Only players who registered have a public profile. If you played with a registered friend
      but never signed up yourself, you show up as a name on their match and nowhere else.</p>

    <h2>How long we keep it</h2>
    <p>Registered players: until you delete your account. Everyone else: data about you is deleted
      automatically when your latest match with us is 60 days old. That gives friends time to sign up
      and keep their old cards.</p>

    <h2>Delete your data</h2>
    <p>Sign in through Steam and press the button below. Everything about you is deleted at once:
      cards, kills, hands, statistics and your account. We then keep only your SteamID on a list,
      so that your future matches are not stored again. Registering later takes you off that list.</p>
    ${signedIn
      ? `<div class="buttons" id="delete-area">
           <button type="button" class="button danger" id="delete-start">Delete my data</button>
         </div>`
      : `<div class="buttons"><a class="button" href="/auth/steam">Sign in through Steam</a></div>`}
    <p class="dim">Questions: open an issue on the project's GitHub page.</p>`);

  if (!signedIn) return;
  // Two steps: the first press only asks, so nobody deletes a season of cards by a slip of the mouse
  document.getElementById('delete-start').addEventListener('click', () => {
    const area = document.getElementById('delete-area');
    area.innerHTML = `
      <p class="error">This deletes all your cards, hands and statistics, and cannot be undone. Are you sure?</p>
      <div class="buttons">
        <button type="button" class="button danger" id="delete-confirm">Yes, delete everything</button>
        <button type="button" class="button" id="delete-cancel">Cancel</button>
      </div>`;
    document.getElementById('delete-cancel').addEventListener('click', privacy);
    document.getElementById('delete-confirm').addEventListener('click', async (event) => {
      event.target.disabled = true;
      try {
        await api('/api/me', { method: 'DELETE' });
        state.me = null; // the server has signed us out too
        drawMenu('/privacy');
        drawWindow('Data deleted', `
          <p>Everything about you has been deleted and you have been signed out.
            Your future matches will not be stored.</p>
          <div class="buttons"><a class="button" href="#/how">How it works</a></div>`);
      } catch (e) {
        area.innerHTML = `<p class="error">${esc(e.message)}</p>`;
      }
    });
  });
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
      await api('/api/accounts', {
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

/**
 * All cards a player has this season, strongest first.
 * Without an id it is your own collection (the menu item); with one, another player's,
 * opened from their hand on a profile or in a comparison.
 */
async function collection(steamId64 = state.me.steamId64) {
  const own = state.me && state.me.steamId64 === steamId64;
  drawWindow('Collection', '<p class="dim">Loading cards...</p>');
  try {
    // The collection does not carry the player's name, so for someone else it comes from the
    // leaderboard: anyone with cards has a hand there
    const [data, entries] = await Promise.all([
      api(`/api/players/${encodeURIComponent(steamId64)}/collection`),
      own ? Promise.resolve([]) : api('/api/seasons/current/leaderboard').catch(() => []),
    ]);
    const entry = entries.find(e => e.steamId64 === steamId64);
    const name = (entry && entry.displayName) || 'This player';
    const cards = [...data.cards].sort((a, b) => b.points - a.points);

    const empty = own
      ? 'No cards yet this season. Play a match, and its cards appear here a few minutes after it is read.'
      : `${esc(name)} has no cards yet this season.`;
    const back = own ? '' : `<div class="buttons">
        <a class="button" href="#/player/${esc(steamId64)}">${esc(name)}'s profile</a>
        <a class="button" href="#/leaderboard">Leaderboard</a>
      </div>`;

    drawWindow(own ? `Collection, ${data.season.name}` : `${name}'s collection, ${data.season.name}`,
      (cards.length === 0
        ? `<p>${empty}</p>`
        : `<p>${cards.length} cards and ${data.kills.length} kills this season, strongest first.</p>
           <div class="cards">${cards.map(c => cardHtml(c)).join('')}</div>`) + back);
  } catch (e) {
    if (e.status === 404) {
      drawWindow('Collection', `<p>This player has no public collection. Only players who signed up
        themselves have one.</p><p class="dim"><a href="#/privacy">Why?</a></p>`);
      return;
    }
    drawError('Collection', e);
  }
}

/** The button under a hand that leads to the cards it was picked from. */
function collectionButton(steamId64, name) {
  const own = state.me && state.me.steamId64 === steamId64;
  const href = own ? '#/collection' : `#/player/${esc(steamId64)}/collection`;
  const text = own ? 'See my collection' : `See ${esc(name)}'s collection`;
  return `<div class="buttons"><a class="button" href="${href}">${text}</a></div>`;
}

/**
 * A season hand: type, points and the cards. Used on "My hand" and on every profile.
 * who: 'you' on your own page, otherwise the player's name, for the "built by" line.
 */
function handHtml(current, who) {
  const items = current.items.map(i => (i.card ? cardHtml(i.card) : killHtml(i.kill))).join('');
  const how = current.custom
    ? `, built by ${who === 'you' ? 'you' : esc(who)}`
    : who === 'you' ? ', the strongest you can make' : ', the strongest from their cards';
  return `<div class="hand-head">
      <span class="hand-type">${esc(current.typeName)}</span>
      <span class="hand-score">${current.score} points</span>
      <span class="dim">${current.totalDamage} damage${how}</span>
    </div>
    <div class="cards">${items}</div>`;
}

/** The season hand on a profile, loaded after the page like the lifetime numbers. */
async function profileHand(steamId64, name) {
  const box = document.getElementById('profile-hand');
  try {
    const current = await api(`/api/players/${encodeURIComponent(steamId64)}/hand`);
    if (!box.isConnected) return;
    box.innerHTML = '<h2>Season hand</h2>' + (current
      ? handHtml(current, name) + collectionButton(steamId64, name)
      : '<p class="dim">No hand yet this season. It appears with the first card.</p>');
  } catch (e) {
    if (box.isConnected) box.innerHTML = '<h2>Season hand</h2><p class="dim">The hand could not be loaded.</p>';
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
    drawWindow(`My hand, ${current.season.name}`, `
      ${handHtml(current, 'you')}
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

/** A number for display: '-' when it could not be calculated (no rounds, no shots ...). */
function num(value, decimals = 0, suffix = '') {
  if (value === null || value === undefined) return '-';
  return Number(value).toFixed(decimals) + suffix;
}

/** Label and value rows, like the server info panel in the old server browser. */
function facts(rows) {
  return `<table class="facts"><tbody>${rows.map(([label, value]) =>
    `<tr><th scope="row">${label}</th><td class="num">${value}</td></tr>`).join('')}</tbody></table>`;
}

async function profile(steamId64) {
  drawWindow('Player', '<p class="dim">Loading the profile...</p>');
  try {
    const p = await api(`/api/players/${encodeURIComponent(steamId64)}/stats`);
    const s = p.stats;
    const name = p.displayName || p.steamId64;

    const body = s.matches === 0
      ? `<p>No matches read this season yet. Statistics appear after the first match with a demo.</p>`
      : `<div class="profile-grid">
          <section>
            <h2>Matches</h2>
            ${facts([
              ['Played', s.matches],
              ['Won, lost, drawn', `${s.wins}, ${s.losses}, ${s.draws}`],
              ['Win rate', num(s.winRate, 0, ' %')],
              ['Rounds', s.roundsPlayed],
              ['MVPs', s.mvps],
            ])}
          </section>
          <section>
            <h2>Aim</h2>
            ${facts([
              ['Kills, deaths', `${s.kills}, ${s.deaths}`],
              ['K/D', num(s.killDeathRatio, 2)],
              ['Headshots', num(s.headshotPercent, 0, ' %')],
              ['Accuracy', num(s.accuracyPercent, 0, ' %')],
              ['Time to kill', num(s.timeToKillSeconds, 2, ' s')],
            ])}
          </section>
          <section>
            <h2>Impact</h2>
            ${facts([
              ['Damage per round', num(s.averageDamagePerRound, 1)],
              ['Utility damage per round', num(s.utilityDamagePerRound, 1)],
              ['Opening kills, deaths', `${s.openingKills}, ${s.openingDeaths}`],
              ['Opening duels won', num(s.openingSuccessPercent, 0, ' %')],
              ['Assists, flash assists', `${s.assists}, ${s.flashAssists}`],
            ])}
          </section>
          <section>
            <h2>Cards</h2>
            ${p.cards.length === 0 ? '<p class="dim">No cards yet.</p>'
              : facts([...p.cards.map(c => [esc(c.name), c.count]), ['Clutches', p.clutches]])}
          </section>
          <section>
            <h2>Kills by weapon type</h2>
            ${p.weapons.length === 0 ? '<p class="dim">No kills yet.</p>'
              : facts(p.weapons.map(w => [esc(w.name), w.count]))}
          </section>
          <section id="lifetime">
            <h2>All time, from Steam</h2>
            <p class="dim">Loading...</p>
          </section>
        </div>`;

    drawWindow(`${name}, ${p.season.name}`, `
      <div class="profile-head">
        ${p.avatarUrl ? `<img class="avatar-large" src="${esc(p.avatarUrl)}" alt="" width="64" height="64">` : ''}
        <div>
          <div class="profile-name">${esc(name)}</div>
          <a class="dim" href="https://steamcommunity.com/profiles/${esc(p.steamId64)}" target="_blank" rel="noopener">Steam profile</a>
        </div>
      </div>
      <section class="profile-hand" id="profile-hand"><h2>Season hand</h2><p class="dim">Loading...</p></section>
      ${body}
      <p class="dim">Premier, Competitive and Wingman matches this season. Exit frags count here, as on the scoreboard; team kills and bots do not.</p>`);

    profileHand(steamId64, name);
    if (s.matches > 0) lifetime(steamId64);
  } catch (e) {
    if (e.status === 404) {
      drawWindow('Player', `<p>This player has no public profile. Only players who signed up
        themselves get one.</p><p class="dim"><a href="#/privacy">Why?</a></p>`);
      return;
    }
    drawError('Player', e);
  }
}

/** Steam's own lifetime numbers, loaded after the page so a slow Steam never holds it up. */
async function lifetime(steamId64) {
  const box = document.getElementById('lifetime');
  try {
    const l = await api(`/api/players/${encodeURIComponent(steamId64)}/lifetime`);
    if (!box.isConnected) return; // the player opened another window meanwhile
    box.innerHTML = '<h2>All time, from Steam</h2>' + (l.available
      ? facts([
          ['Kills, deaths', `${l.kills ?? '-'}, ${l.deaths ?? '-'}`],
          ['K/D', num(l.killDeathRatio, 2)],
          ['Headshots', num(l.headshotPercent, 0, ' %')],
          ['Accuracy', num(l.accuracyPercent, 0, ' %')],
          ['Matches won', l.matchesWon ?? '-'],
          ['Hours played', l.hoursPlayed ?? '-'],
        ]) + '<p class="hint">Every game mode since the account was created, including CS:GO.</p>'
      : '<p class="dim">Not shared. Steam only shows these when the profile\'s game details are public.</p>');
  } catch (e) {
    if (box.isConnected) box.innerHTML = '<h2>All time, from Steam</h2><p class="dim">Steam did not answer.</p>';
  }
}

async function leaderboard() {
  drawWindow('Leaderboard', '<p class="dim">Loading the leaderboard...</p>');
  try {
    const [season, entries] = await Promise.all([
      api('/api/seasons/current'),
      api('/api/seasons/current/leaderboard'),
    ]);
    // Only registered players have a public hand, so only they get a box to tick
    const rows = entries.map(e => `<tr>
        <td class="pick">${e.registered
          ? `<input type="checkbox" class="compare-pick" value="${esc(e.steamId64)}"
              aria-label="Compare ${esc(e.displayName || e.steamId64)}">`
          : ''}</td>
        <td class="num">${e.rank}</td>
        <td class="player">${avatar(e.avatarUrl)}${e.registered
          ? `<a href="#/player/${esc(e.steamId64)}">${esc(e.displayName || e.steamId64)}</a>`
          : esc(e.displayName || 'Unknown player')}</td>
        <td>${esc(e.typeName)}</td>
        <td class="num">${e.score}</td>
      </tr>`).join('');
    drawWindow(`Leaderboard, ${season.name}`, entries.length === 0
      ? '<p>No hands yet this season.</p>'
      : `<div class="compare-bar">
          <span id="compare-status" class="dim">Tick two players to compare their hands side by side.</span>
          <button class="button" type="button" id="compare" disabled>Compare</button>
        </div>
        <div class="table-wrap"><table>
          <thead><tr><th class="pick"><span class="visually-hidden">Compare</span></th><th class="num">#</th>
            <th>Player</th><th>Hand</th><th class="num">Points</th></tr></thead>
          <tbody>${rows}</tbody>
        </table></div>`);

    if (entries.length > 0) wireComparePicks(entries);
  } catch (e) {
    drawError('Leaderboard', e);
  }
}

/** The tick boxes on the leaderboard: at most two at a time, then Compare opens both hands. */
function wireComparePicks(entries) {
  const boxes = [...document.querySelectorAll('.compare-pick')];
  const button = document.getElementById('compare');
  const status = document.getElementById('compare-status');
  const nameOf = id => {
    const entry = entries.find(e => e.steamId64 === id);
    return (entry && entry.displayName) || id;
  };

  const update = () => {
    const picked = boxes.filter(b => b.checked).map(b => b.value);
    // With two ticked, the rest are locked, so nobody can pick a third by mistake
    boxes.forEach(b => { b.disabled = !b.checked && picked.length >= 2; });
    button.disabled = picked.length !== 2;
    status.textContent = picked.length === 0
      ? 'Tick two players to compare their hands side by side.'
      : picked.length === 1
        ? `${nameOf(picked[0])} against... tick one more.`
        : `${nameOf(picked[0])} against ${nameOf(picked[1])}`;
  };

  boxes.forEach(b => b.addEventListener('change', update));
  button.addEventListener('click', () => {
    const [first, second] = boxes.filter(b => b.checked).map(b => b.value);
    location.hash = `#/compare/${first}/${second}`; // a link of its own, so it can be shared
  });
}

/** Which of two hands is stronger: points first, then damage, the same order as the leaderboard. */
function strongerHand(first, second) {
  if (first.score !== second.score) return first.score > second.score ? 0 : 1;
  if (first.totalDamage !== second.totalDamage) return first.totalDamage > second.totalDamage ? 0 : 1;
  return -1; // exactly even
}

/** Two season hands side by side, opened from the leaderboard. */
async function compare(firstId, secondId) {
  drawWindow('Compare hands', '<p class="dim">Loading both hands...</p>');
  // One hand failing to load should not hide the other, so each catches its own error
  const handOf = id => api(`/api/players/${id}/hand`).catch(error => ({ error }));
  try {
    const [entries, first, second] = await Promise.all([
      api('/api/seasons/current/leaderboard'),
      handOf(firstId),
      handOf(secondId),
    ]);
    const hands = [first, second];
    const ids = [firstId, secondId];
    const bothReady = hands.every(h => h && !h.error);
    const winner = bothReady ? strongerHand(first, second) : -1;

    const side = (i) => {
      const id = ids[i];
      const current = hands[i];
      const entry = entries.find(e => e.steamId64 === id);
      const name = (entry && entry.displayName) || id;
      const body = current && current.error
        ? `<p class="dim">${esc(current.error.message)}</p>`
        : !current
          ? '<p class="dim">No hand yet this season.</p>'
          : handHtml(current, name) + collectionButton(id, name);
      return `<section class="compare-side${winner === i ? ' winner' : ''}">
          <div class="compare-player">
            ${avatar(entry && entry.avatarUrl)}
            <a href="#/player/${esc(id)}">${esc(name)}</a>
            ${entry ? `<span class="dim">#${entry.rank}</span>` : ''}
            ${winner === i ? '<span class="compare-badge">Stronger hand</span>' : ''}
          </div>
          ${body}
        </section>`;
    };

    let verdict = '';
    if (bothReady) {
      const names = ids.map(id => {
        const entry = entries.find(e => e.steamId64 === id);
        return esc((entry && entry.displayName) || id);
      });
      const pointsGap = Math.abs(first.score - second.score);
      const damageGap = Math.abs(first.totalDamage - second.totalDamage);
      verdict = winner === -1
        ? 'Exactly even: same points and the same damage.'
        : pointsGap > 0
          ? `${names[winner]} is ahead by ${pointsGap} points.`
          : `Same points. ${names[winner]} is ahead on damage, by ${damageGap}.`;
    }

    drawWindow('Compare hands', `
      ${verdict ? `<p class="compare-verdict">${verdict}</p>` : ''}
      <div class="compare">${side(0)}${side(1)}</div>
      <div class="buttons"><a class="button" href="#/leaderboard">Back to the leaderboard</a></div>`);
  } catch (e) {
    drawError('Compare hands', e);
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
  '/signin-cancelled': signInCancelled,
  '/signin-failed': signInFailed,
  '/privacy': privacy,
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

  const pair = route.match(/^\/compare\/(\d{17})\/(\d{17})$/);
  drawMenu(pair ? '/leaderboard' : route); // comparing is part of the leaderboard
  if (pair) return compare(pair[1], pair[2]);
  const player = route.match(/^\/player\/(\d{17})$/);
  if (player) return profile(player[1]);
  const theirCards = route.match(/^\/player\/(\d{17})\/collection$/);
  if (theirCards) return collection(theirCards[1]);
  (ROUTES[route] || howItWorks)();
}

window.addEventListener('hashchange', showRoute);
loadMe().then(showRoute);
