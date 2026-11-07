# Draws docs/architecture.png and docs/architecture-sv.png.
# Usage: python3 docs/draw_architecture.py en docs/architecture.png
#        python3 docs/draw_architecture.py sv docs/architecture-sv.png
# Requires Pillow and the Lato font.
import sys, math
from PIL import Image, ImageDraw, ImageFont

LANG = sys.argv[1]
OUT = sys.argv[2]
F = "/usr/share/fonts/truetype/lato/Lato-%s.ttf"
def font(w, s): return ImageFont.truetype(F % w, s)

W, H = 5200, 3440
BG=(247,246,242); TEXT=(31,35,40); MUTED=(92,98,106); LINE=(128,128,128)
PAL = {
 'rules':   dict(fill=(251,242,225), border=(211,173,115), tag=(170,112,30)),
 'input':   dict(fill=(214,233,220), border=(142,198,166), tag=(52,115,80)),
 'output':  dict(fill=(226,236,252), border=(136,173,234), tag=(45,90,165)),
 'platform':dict(fill=(237,235,231), border=(152,153,153), tag=(85,88,92)),
 'callout': dict(fill=(240,234,250), border=(182,157,222), tag=(105,75,165)),
}

T = {
'en': dict(
 title="CS2Holdem",
 sub="How the app fits together \u2013 the hand engine in the center turns CS2 kills into poker hands",
 center_tag="CENTER \u00b7 PURE JAVA", center_name="Hand engine",
 center_desc="Detects events in a round, finds a player's strongest hand and compares hands.",
 center_q="Why does the engine know nothing about Spring or the database?",
 center_how="Plain Java: records, enums and classes. Events go in, hands come out.",
 center_why="The rules are the game. They can be unit tested in milliseconds, and storage or APIs can change without touching a single rule.",
 rules_tag="RULES \u00b7 DOCS/CONCEPT.MD", rules_name="What the engine knows",
 rules=["Events: 2k, 3k, 4k, ace or a special kill \u2013 always within one round",
        "Hands: high card, pair \u2026 full house \u2026 five of a kind, royal flush",
        "Stamps: Smoked, Blind, One shot \u2026 add points",
        "Flush: five kills in the same area within five rounds in a row",
        "Straights: a kill in five rounds in a row, five different weapons",
        "Balance: points from rarity, on a capped logarithmic scale"],
 input_h="INPUT \u2013 where the cards come from",
 inputs=[("EXTERNAL","Steam Web API","Valve's official source for a player's match history",False),
         ("MODULE","steam","Verifies users and decodes share codes into match IDs",False),
         ("MODULE","sync","Follows the share code chain and decides when each user is synced next",False),
         ("INFRASTRUCTURE","Job queue","Holds \u201cdownload and parse this demo\u201d jobs",True),
         ("WORKER","Demo worker","Downloads a demo and turns every kill into events and stamps",True),
         ("EXTERNAL","CS2 Game Coordinator","Provides the download link for a match demo",True)],
 in_edges=["share codes","","new matches","jobs","demo link"],
 output_h="OUTPUT \u2013 what players see",
 outputs=[("MODULE","api","REST endpoints with validated input and clear error messages",False),
          ("FEATURE","Season hand","One per player and season \u2013 suggested automatically or built by hand, locked at season end",True),
          ("FEATURE","Card collection","Every card and season hand ever collected. Nothing is deleted.",True),
          ("FEATURE","Leaderboards","The strongest hands of the season and a hall of fame",True),
          ("FEATURE","Replays","Watch every event in CS2 or as a 2D replay in the browser",True)],
 platform_h="PLATFORM \u2013 what it runs on",
 platform=[("Spring Boot","Modular monolith: api, account, sync, match, steam, hand",False),
           ("PostgreSQL + Flyway","Every schema change is a versioned migration",False),
           ("Docker Compose","The database starts automatically with the app",False),
           ("S3","Demo files and replay data",True),
           ("AWS","RDS, ECS, SQS and Secrets Manager",True)],
 l_defines="defines", l_runs="runs on", l_events="events = cards", l_hands="hands",
 how="How:", why="Why:",
 callouts=[
  ("SYNC \u2013 RATE LIMITS","Why isn't every player checked every minute?",
   "Each user has a next sync time: soon after new matches, later when nothing happened.",
   "Steam allows about 100,000 calls a day. 50,000 users checked every 5 minutes would need 14.4 million."),
  ("SECRETS \u2013 .ENV","Where are the passwords and API keys?",
   "In a git-ignored .env file locally, and in AWS Secrets Manager in production. Sensitive fields are masked in toString().",
   "Nothing secret can leak to GitHub or end up in a log file."),
  ("JOB QUEUE \u2013 STATELESS","Why don't demos download while the user waits?",
   "Sync puts a job in a queue. A worker picks it up and stores the files in S3.",
   "Parsing takes seconds and lots of CPU. With no files on the server, any number of app instances and workers can run side by side."),
  ("FLYWAY \u2013 VERSIONED SCHEMA","Why doesn't Hibernate create the tables?",
   "Every change is a numbered SQL file (V1, V2 \u2026). Hibernate only validates that the entities match.",
   "Changes are reviewed in git and safe to run on a live database full of players' data.")],
 legend_solid="Solid border = exists today", legend_dashed="Dashed border = planned"),
'sv': dict(
 title="CS2Holdem",
 sub="S\u00e5 h\u00e4nger appen ihop \u2013 hand-motorn i mitten g\u00f6r CS2-kills till pokerh\u00e4nder",
 center_tag="CENTRUM \u00b7 REN JAVA", center_name="Hand-motorn",
 center_desc="Hittar h\u00e4ndelser i en runda, tar fram spelarens starkaste hand och j\u00e4mf\u00f6r h\u00e4nder.",
 center_q="Varf\u00f6r vet motorn ingenting om Spring eller databasen?",
 center_how="Ren Java: records, enums och klasser. H\u00e4ndelser in, h\u00e4nder ut.",
 center_why="Reglerna \u00e4r spelet. De kan enhetstestas p\u00e5 millisekunder, och lagring eller API:er kan \u00e4ndras utan att en enda regel r\u00f6rs.",
 rules_tag="REGLER \u00b7 DOCS/CONCEPT.MD", rules_name="Det motorn kan",
 rules=["H\u00e4ndelser: 2k, 3k, 4k, ace eller en speciell kill \u2013 alltid inom en runda",
        "H\u00e4nder: h\u00f6gt kort, par \u2026 k\u00e5k \u2026 femtal, royal flush",
        "St\u00e4mplar: Smoked, Blind, One shot \u2026 ger extra po\u00e4ng",
        "F\u00e4rg: fem kills p\u00e5 samma plats inom fem rundor i rad",
        "Stegar: en kill i fem rundor i rad, med fem olika vapen",
        "Balans: po\u00e4ng efter hur s\u00e4llsynt n\u00e5got \u00e4r, logaritmisk skala med tak"],
 input_h="IN \u2013 var korten kommer ifr\u00e5n",
 inputs=[("EXTERN","Steam Web API","Valves officiella k\u00e4lla f\u00f6r en spelares matchhistorik",False),
         ("MODUL","steam","Verifierar anv\u00e4ndare och avkodar share codes till match-ID",False),
         ("MODUL","sync","F\u00f6ljer kedjan av share codes och best\u00e4mmer n\u00e4r varje anv\u00e4ndare synkas n\u00e4st",False),
         ("INFRASTRUKTUR","Jobbk\u00f6","H\u00e5ller jobben \u201eladda ner och parsa den h\u00e4r demon\u201c",True),
         ("WORKER","Demo-worker","Laddar ner en demo och g\u00f6r varje kill till h\u00e4ndelser och st\u00e4mplar",True),
         ("EXTERN","CS2 Game Coordinator","Ger nedladdningsl\u00e4nken till en matchs demo",True)],
 in_edges=["share codes","","nya matcher","jobb","demo-l\u00e4nk"],
 output_h="UT \u2013 det spelarna ser",
 outputs=[("MODUL","api","REST-endpoints med validerad input och tydliga felmeddelanden",False),
          ("FUNKTION","S\u00e4songshand","En per spelare och s\u00e4song \u2013 f\u00f6reslagen automatiskt eller byggd sj\u00e4lv, l\u00e5st n\u00e4r s\u00e4songen slutar",True),
          ("FUNKTION","Kortsamling","Alla kort och s\u00e4songsh\u00e4nder n\u00e5gonsin. Inget raderas.",True),
          ("FUNKTION","Topplistor","S\u00e4songens starkaste h\u00e4nder och en hall of fame",True),
          ("FUNKTION","Replays","Se varje h\u00e4ndelse i CS2 eller som 2D-replay i webbl\u00e4saren",True)],
 platform_h="PLATTFORM \u2013 det allt k\u00f6rs p\u00e5",
 platform=[("Spring Boot","Modul\u00e4r monolit: api, account, sync, match, steam, hand",False),
           ("PostgreSQL + Flyway","Varje \u00e4ndring i databasen \u00e4r en versionerad migrering",False),
           ("Docker Compose","Databasen startar automatiskt med appen",False),
           ("S3","Demofiler och replay-data",True),
           ("AWS","RDS, ECS, SQS och Secrets Manager",True)],
 l_defines="best\u00e4mmer", l_runs="k\u00f6rs p\u00e5", l_events="h\u00e4ndelser = kort", l_hands="h\u00e4nder",
 how="Hur:", why="Varf\u00f6r:",
 callouts=[
  ("SYNK \u2013 ANROPSGR\u00c4NS","Varf\u00f6r kollas inte alla spelare varje minut?",
   "Varje anv\u00e4ndare har en tid f\u00f6r n\u00e4sta synk: snart efter nya matcher, senare n\u00e4r inget h\u00e4nt.",
   "Steam till\u00e5ter ungef\u00e4r 100 000 anrop per dygn. 50 000 anv\u00e4ndare var femte minut skulle kr\u00e4va 14,4 miljoner."),
  ("HEMLIGHETER \u2013 .ENV","Var finns l\u00f6senorden och API-nycklarna?",
   "I en .env-fil som git ignorerar lokalt, och i AWS Secrets Manager i produktion. K\u00e4nsliga f\u00e4lt d\u00f6ljs i toString().",
   "Inget hemligt kan l\u00e4cka till GitHub eller hamna i en loggfil."),
  ("JOBBK\u00d6 \u2013 STATELESS","Varf\u00f6r laddas inte demos ner medan anv\u00e4ndaren v\u00e4ntar?",
   "Synken l\u00e4gger ett jobb i en k\u00f6. En worker plockar upp det och sparar filerna i S3.",
   "Att parsa tar sekunder och mycket CPU. N\u00e4r inga filer ligger p\u00e5 servern kan hur m\u00e5nga app-instanser och workers som helst k\u00f6ras samtidigt."),
  ("FLYWAY \u2013 VERSIONERAD DATABAS","Varf\u00f6r skapar inte Hibernate tabellerna?",
   "Varje \u00e4ndring \u00e4r en numrerad SQL-fil (V1, V2 \u2026). Hibernate kontrollerar bara att entiteterna st\u00e4mmer.",
   "\u00c4ndringar granskas i git och kan k\u00f6ras s\u00e4kert mot en levande databas full med spelares data.")],
 legend_solid="Heldragen kant = finns i dag", legend_dashed="Streckad kant = planerad"),
}[LANG]

img = Image.new("RGB",(W,H),BG)
d = ImageDraw.Draw(img)
RECTS = []

def tw(t,f): return d.textlength(t,font=f)
def wrap(text,f,maxw):
    words=text.split(' '); lines=[]; cur=''
    for w in words:
        t=(cur+' '+w).strip()
        if tw(t,f)<=maxw: cur=t
        else:
            if cur: lines.append(cur)
            cur=w
    if cur: lines.append(cur)
    return lines
def lh(f): a,b=f.getmetrics(); return int((a+b)*1.18)

def perimeter(x0,y0,x1,y1,r,step=4):
    pts=[]
    def arc(cx,cy,a0,a1):
        n=max(4,int(abs(a1-a0)*r/step))
        for i in range(n+1):
            a=math.radians(a0+(a1-a0)*i/n); pts.append((cx+r*math.cos(a),cy+r*math.sin(a)))
    def seg(p,q):
        L=math.dist(p,q); n=max(1,int(L/step))
        for i in range(n+1): pts.append((p[0]+(q[0]-p[0])*i/n,p[1]+(q[1]-p[1])*i/n))
    seg((x0+r,y0),(x1-r,y0)); arc(x1-r,y0+r,-90,0)
    seg((x1,y0+r),(x1,y1-r)); arc(x1-r,y1-r,0,90)
    seg((x1-r,y1),(x0+r,y1)); arc(x0+r,y1-r,90,180)
    seg((x0,y1-r),(x0,y0+r)); arc(x0+r,y0+r,180,270)
    return pts

def box(x0,y0,x1,y1,fill,border,dashed=False,r=30,bw=5):
    RECTS.append((x0,y0,x1,y1))
    d.rounded_rectangle((x0,y0,x1,y1),radius=r,fill=fill,outline=None if dashed else border,width=bw)
    if dashed:
        pts=perimeter(x0,y0,x1,y1,r); dash=26; gap=18; acc=0; on=True; prev=pts[0]
        for p in pts[1:]:
            seglen=math.dist(prev,p); acc+=seglen
            if on: d.line((prev,p),fill=border,width=bw)
            if (on and acc>=dash) or (not on and acc>=gap): on=not on; acc=0
            prev=p

def pill(cx,cy,text,f=None,fg=MUTED):
    f=f or font('Semibold',34)
    w=tw(text,f); h=lh(f)
    x0,y0,x1,y1=cx-w/2-22,cy-h/2-6,cx+w/2+22,cy+h/2+6
    d.rounded_rectangle((x0,y0,x1,y1),radius=int(h/2),fill=BG,outline=LINE,width=3)
    d.text((cx,cy),text,font=f,fill=fg,anchor='mm')

def arrow(p,q,label=None,head=True,lp=0.5,width=6):
    d.line((p,q),fill=LINE,width=width)
    if head:
        ang=math.atan2(q[1]-p[1],q[0]-p[0]); L=30; s=0.45
        a=(q[0]-L*math.cos(ang-s),q[1]-L*math.sin(ang-s)); b=(q[0]-L*math.cos(ang+s),q[1]-L*math.sin(ang+s))
        d.polygon([q,a,b],fill=LINE)
    if label: pill(p[0]+(q[0]-p[0])*lp,p[1]+(q[1]-p[1])*lp,label)

# ---------- node measuring/drawing
TAGF=font('Black',30); NAMEF=font('Black',58); DESCF=font('Regular',40)
def node_h(name,desc,w,pad=40,namef=NAMEF,descf=DESCF):
    return pad+lh(TAGF)+8+lh(namef)+6+len(wrap(desc,descf,w-2*pad))*lh(descf)+pad-10
def node(x,y,w,tag,name,desc,pal,planned,pad=40,namef=NAMEF,descf=DESCF):
    h=node_h(name,desc,w,pad,namef,descf)
    box(x,y,x+w,y+h,pal['fill'],pal['border'],dashed=planned)
    cy=y+pad
    if tag: d.text((x+pad,cy),tag,font=TAGF,fill=pal['tag'])
    if planned:
        pt=("PLANERAD" if LANG=='sv' else "PLANNED"); f=font('Black',26)
        pw=tw(pt,f)+36; px=(x+w-pad-pw) if tag else x+pad
        d.rounded_rectangle((px,cy-4,px+pw,cy+lh(f)+2),radius=20,outline=pal['tag'],width=3)
        d.text((px+18,cy),pt,font=f,fill=pal['tag'])
    cy+=lh(TAGF)+8
    d.text((x+pad,cy),name,font=namef,fill=TEXT); cy+=lh(namef)+6
    for ln in wrap(desc,descf,w-2*pad):
        d.text((x+pad,cy),ln,font=descf,fill=MUTED); cy+=lh(descf)
    return (x,y,x+w,y+h)

# ---------- callout
CQ=font('Black',46); CT=font('Regular',38); CB=font('Black',38)
def rich_lines(label,text,maxw):
    # first line starts after the bold label
    words=text.split(' '); lines=[]; cur=''; first=True
    for wd in words:
        t=(cur+' '+wd).strip()
        avail=maxw-(tw(label+' ',CB) if first else 0)
        if tw(t,CT)<=avail: cur=t
        else:
            lines.append(cur); cur=wd; first=False
    lines.append(cur); return lines
def callout_h(c,w,pad=44):
    tag,q,how,why=c; mw=w-2*pad
    h=pad+lh(TAGF)+10+len(wrap(q,CQ,mw))*lh(CQ)+18
    h+=len(rich_lines(T['how'],how,mw))*lh(CT)+14+len(rich_lines(T['why'],why,mw))*lh(CT)+pad-8
    return h
def callout(x,y,w,c,pad=44,pal=PAL['callout']):
    tag,q,how,why=c; mw=w-2*pad; h=callout_h(c,w,pad)
    box(x,y,x+w,y+h,pal['fill'],pal['border'])
    cy=y+pad; d.text((x+pad,cy),tag,font=TAGF,fill=pal['tag']); cy+=lh(TAGF)+10
    for ln in wrap(q,CQ,mw): d.text((x+pad,cy),ln,font=CQ,fill=TEXT); cy+=lh(CQ)
    cy+=18
    for label,txt in ((T['how'],how),(T['why'],why)):
        lines=rich_lines(label,txt,mw)
        for i,ln in enumerate(lines):
            if i==0:
                d.text((x+pad,cy),label,font=CB,fill=TEXT)
                d.text((x+pad+tw(label+' ',CB),cy),ln,font=CT,fill=TEXT)
            else: d.text((x+pad,cy),ln,font=CT,fill=TEXT)
            cy+=lh(CT)
        cy+=14
    return (x,y,x+w,y+h)

# ---------- layout
M=160
d.text((M,90),T['title'],font=font('Black',104),fill=TEXT)
d.text((M,225),T['sub'],font=font('Regular',50),fill=MUTED)
TOP=380; BOTTOM=H-150
CW=1240
c=T['callouts']
tl=callout(M,TOP,CW,c[0]); tr=callout(W-M-CW,TOP,CW,c[1])
bl_h=callout_h(c[2],CW); br_h=callout_h(c[3],CW)
bl=callout(M,BOTTOM-bl_h,CW,c[2]); br=callout(W-M-CW,BOTTOM-br_h,CW,c[3])

# rules (top centre)
RX0=M+CW+150; RX1=W-M-CW-150; RW=RX1-RX0; pad=44
RNF=font('Black',52); RBF=font('Regular',38)
colw=(RW-2*pad-60)/2
cols=[T['rules'][:3],T['rules'][3:]]
def bul_h(items): return sum(len(wrap(it,RBF,colw-40))*lh(RBF)+14 for it in items)
rh=pad+lh(TAGF)+8+lh(RNF)+16+max(bul_h(cols[0]),bul_h(cols[1]))+pad-14
pr=PAL['rules']; box(RX0,TOP,RX1,TOP+rh,pr['fill'],pr['border'])
cy=TOP+pad; d.text((RX0+pad,cy),T['rules_tag'],font=TAGF,fill=pr['tag']); cy+=lh(TAGF)+8
d.text((RX0+pad,cy),T['rules_name'],font=RNF,fill=TEXT); cy+=lh(RNF)+16
for ci,items in enumerate(cols):
    yy=cy; xx=RX0+pad+ci*(colw+60)
    for it in items:
        d.ellipse((xx,yy+18,xx+14,yy+32),fill=pr['tag'])
        for ln in wrap(it,RBF,colw-40):
            d.text((xx+34,yy),ln,font=RBF,fill=TEXT); yy+=lh(RBF)
        yy+=14
rules=(RX0,TOP,RX1,TOP+rh)

# centre
CX0=2000; CX1=W-2000; CWID=CX1-CX0
cpad=50; CNF=font('Black',84); CDF=font('Regular',42); CQF=font('Black',44); CTF=font('Regular',40); CBF=font('Black',40)
mw=CWID-2*cpad
def cwrap_rich(label,text):
    words=text.split(' '); lines=[]; cur=''; first=True
    for wd in words:
        t=(cur+' '+wd).strip(); avail=mw-(tw(label+' ',CBF) if first else 0)
        if tw(t,CTF)<=avail: cur=t
        else: lines.append(cur); cur=wd; first=False
    lines.append(cur); return lines
ch=cpad+lh(TAGF)+8+lh(CNF)+8+len(wrap(T['center_desc'],CDF,mw))*lh(CDF)+30+4+30
ch+=len(wrap(T['center_q'],CQF,mw))*lh(CQF)+14+len(cwrap_rich(T['how'],T['center_how']))*lh(CTF)+12+len(cwrap_rich(T['why'],T['center_why']))*lh(CTF)+cpad-10
CY0=1500; CY1=CY0+ch
d.rounded_rectangle((CX0+14,CY0+18,CX1+14,CY1+18),radius=40,fill=(222,220,214))
box(CX0,CY0,CX1,CY1,TEXT,TEXT,r=40)
cy=CY0+cpad; d.text((CX0+cpad,cy),T['center_tag'],font=TAGF,fill=(235,190,110)); cy+=lh(TAGF)+8
d.text((CX0+cpad,cy),T['center_name'],font=CNF,fill=(255,255,255)); cy+=lh(CNF)+8
for ln in wrap(T['center_desc'],CDF,mw): d.text((CX0+cpad,cy),ln,font=CDF,fill=(215,218,222)); cy+=lh(CDF)
cy+=30; d.line((CX0+cpad,cy,CX1-cpad,cy),fill=(80,86,94),width=3); cy+=30
for ln in wrap(T['center_q'],CQF,mw): d.text((CX0+cpad,cy),ln,font=CQF,fill=(255,255,255)); cy+=lh(CQF)
cy+=14
for label,txt in ((T['how'],T['center_how']),(T['why'],T['center_why'])):
    for i,ln in enumerate(cwrap_rich(label,txt)):
        if i==0:
            d.text((CX0+cpad,cy),label,font=CBF,fill=(235,190,110)); d.text((CX0+cpad+tw(label+' ',CBF),cy),ln,font=CTF,fill=(230,232,235))
        else: d.text((CX0+cpad,cy),ln,font=CTF,fill=(230,232,235))
        cy+=lh(CTF)
    cy+=12
center=(CX0,CY0,CX1,CY1)

# input column (left)
HF=font('Black',40)
IX=M; IW=CW-60; gap=78
iy=tl[3]+110
d.text((IX,iy-62),T['input_h'],font=HF,fill=PAL['input']['tag'])
inodes=[]
NF2=font('Black',50); DF2=font('Regular',37)
for tag,name,desc,pl in T['inputs']:
    r=node(IX,iy,IW,tag,name,desc,PAL['input'],pl,pad=34,namef=NF2,descf=DF2); inodes.append(r); iy=r[3]+gap
assert inodes[-1][3] < bl[1]-40, ("input overflow", inodes[-1][3], bl[1])
for i,lab in enumerate(T['in_edges']):
    a=inodes[i]; b=inodes[i+1]; x=IX+120
    if i==4:  # coordinator -> worker (upwards)
        arrow((x,b[1]),(x,a[3]),lab or None)
    else:
        arrow((x,a[3]),(x,b[1]),lab or None)

# output column (right)
OX=W-M-(CW-60); OW=CW-60
oy=tr[3]+110
d.text((OX,oy-62),T['output_h'],font=HF,fill=PAL['output']['tag'])
onodes=[]
for tag,name,desc,pl in T['outputs']:
    r=node(OX,oy,OW,tag,name,desc,PAL['output'],pl,pad=34,namef=NF2,descf=DF2); onodes.append(r); oy=r[3]+gap*0.6
assert onodes[-1][3] < br[1]-40, ("output overflow", onodes[-1][3], br[1])
spx=OX-70
ys=[(r[1]+r[3])/2 for r in onodes]
d.line((spx,ys[0],spx,ys[-1]),fill=LINE,width=6)
for y in ys: arrow((spx,y),(OX,y),head=True)

# platform (bottom centre)
PX0=RX0; PX1=RX1; n=len(T['platform']); pg=36; pw=(PX1-PX0-(n-1)*pg)/n
PNF=font('Black',42); PDF=font('Regular',34)
ph=max(node_h(nm,ds,int(pw),pad=30,namef=PNF,descf=PDF) for nm,ds,_ in T['platform'])
py1=BOTTOM-130; py0=py1-ph
d.text((PX0,py0-62),T['platform_h'],font=HF,fill=PAL['platform']['tag'])
pnodes=[]
for i,(nm,ds,pl) in enumerate(T['platform']):
    x=PX0+i*(pw+pg)
    tag=None  # cards are narrow: only the PLANNED pill is shown on the tag line
    r=node(int(x),py0,int(pw),tag,nm,ds,PAL['platform'],pl,pad=30,namef=PNF,descf=PDF)
    # equalise height
    pnodes.append(r)
assert py0-80 > CY1+120, ("platform too close to centre", py0, CY1)

# connections to centre
ccx=(CX0+CX1)/2
arrow((ccx,rules[3]),(ccx,CY0),T['l_defines'])
arrow((ccx,CY1),(ccx,py0-80),T['l_runs'],head=False)
d.line((PX0+pw/2,py0-80,PX1-pw/2,py0-80),fill=LINE,width=6)
for r in pnodes:
    mx=(r[0]+r[2])/2; arrow((mx,py0-80),(mx,py0-10))
w=inodes[4]; wy=(w[1]+w[3])/2; ex=(w[2]+CX0)/2
d.line((w[2],wy,ex,wy),fill=LINE,width=6); d.line((ex,wy,ex,(CY0+CY1)/2),fill=LINE,width=6)
arrow((ex,(CY0+CY1)/2),(CX0,(CY0+CY1)/2))
pill(ex,(wy+(CY0+CY1)/2)/2,T['l_events'])
arrow((CX1,(CY0+CY1)/2),(spx,(CY0+CY1)/2),T['l_hands'],head=False)

# legend
LF=font('Semibold',36); ly=BOTTOM-50; lx=PX0
box(lx,ly-26,lx+90,ly+26,BG,LINE,r=12,bw=4); d.text((lx+110,ly),T['legend_solid'],font=LF,fill=MUTED,anchor='lm')
lx2=lx+130+tw(T['legend_solid'],LF)+80
box(lx2,ly-26,lx2+90,ly+26,BG,LINE,dashed=True,r=12,bw=4); d.text((lx2+110,ly),T['legend_dashed'],font=LF,fill=MUTED,anchor='lm')

# overlap check
def inter(a,b): return not (a[2]<=b[0] or b[2]<=a[0] or a[3]<=b[1] or b[3]<=a[1])
bad=[(i,j) for i in range(len(RECTS)) for j in range(i+1,len(RECTS)) if inter(RECTS[i],RECTS[j])
     and not (RECTS[i][0]<=RECTS[j][0] and RECTS[i][2]>=RECTS[j][2] and RECTS[i][1]<=RECTS[j][1] and RECTS[i][3]>=RECTS[j][3])]
print("overlaps:",[(RECTS[i],RECTS[j]) for i,j in bad])
print("centre",center,"rules",rules,"platform top",py0,"input end",inodes[-1][3],"bl top",bl[1],"output end",onodes[-1][3],"br top",br[1])
img.save(OUT,optimize=True)
print("saved",OUT)
