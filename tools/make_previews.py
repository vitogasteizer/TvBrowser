#!/usr/bin/env python3
"""Draws the UI preview images in docs/images/<lang>/ from the string files in app/src/main/res.
Run from the project root:  python3 tools/make_previews.py   (needs Pillow and the DejaVu fonts)."""
import re, math, os, glob
from PIL import Image, ImageDraw, ImageFont
BG=(14,17,22);SURF=(23,27,34);SURF2=(34,40,51);LINE=(38,44,54);TEXT=(242,244,248);MUTED=(154,164,178);ACC=(110,168,255);ONACC=(11,18,32);GRAY=(58,67,82)
FR='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';FB='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
S=2
RES='app/src/main/res/'
def strings(path):
    t=open(path).read()
    return {m.group(1):m.group(2).replace("\\'","'").replace('\\n','\n').replace('&amp;','&').replace('&lt;','<') for m in re.finditer(r'<string name="([a-z_0-9]+)">(.*?)</string>',t,re.S)}
class Cv:
    def __init__(s,w,h):
        s.w,s.h=w,h; s.im=Image.new('RGB',(w*S,h*S),BG); s.d=ImageDraw.Draw(s.im); s.f={}
    def font(s,sz,b=False):
        k=(sz,b)
        if k not in s.f: s.f[k]=ImageFont.truetype(FB if b else FR,int(sz*S))
        return s.f[k]
    def rr(s,x0,y0,x1,y1,r,fill=None,outline=None,w=1):
        s.d.rounded_rectangle([x0*S,y0*S,x1*S,y1*S],radius=r*S,fill=fill,outline=outline,width=int(w*S))
    def circ(s,cx,cy,r,fill=None,outline=None,w=1):
        s.d.ellipse([(cx-r)*S,(cy-r)*S,(cx+r)*S,(cy+r)*S],fill=fill,outline=outline,width=int(w*S))
    def ln(s,pts,col,w=2.2):
        s.d.line([(x*S,y*S) for x,y in pts],fill=col,width=int(w*S),joint='curve')
    def tx(s,x,y,t,sz=14,col=TEXT,b=False,anchor='lm'):
        s.d.text((x*S,y*S),t,font=s.font(sz,b),fill=col,anchor=anchor)
    def save(s,p):
        os.makedirs(os.path.dirname(p),exist_ok=True); s.im.resize((s.w,s.h),Image.LANCZOS).save(p)
    def icon(s,n,cx,cy,col):
        L=lambda pts:s.ln([(cx+x,cy+y) for x,y in pts],col)
        if n=='back':L([(3,-8),(-4,0),(3,8)])
        elif n=='fwd':L([(-3,-8),(4,0),(-3,8)])
        elif n=='reload':
            s.d.arc([(cx-8)*S,(cy-8)*S,(cx+8)*S,(cy+8)*S],40,340,fill=col,width=int(2.2*S)); L([(5,-9),(8,-5),(3,-4)])
        elif n=='home':L([(-10,0),(0,-9),(10,0)]);L([(-7,-2),(-7,9),(-2,9),(-2,3),(2,3),(2,9),(7,9),(7,-2)])
        elif n=='bm':L([(-6,-9),(6,-9),(6,9),(0,4),(-6,9),(-6,-9)])
        elif n=='hist':s.circ(cx,cy,9,outline=col,w=2.2);L([(0,-5),(0,0),(3,2)])
        elif n=='folder':L([(-10,-6),(-4,-6),(-2,-3),(10,-3),(10,8),(-10,8),(-10,-6)])
        elif n=='qr':
            for ox,oy in((-9,-9),(2,-9),(-9,2)):s.rr(cx+ox,cy+oy,cx+ox+7,cy+oy+7,1,outline=col,w=2)
            L([(2,3),(4,3)]);L([(7,3),(9,3)]);L([(2,6),(5,6)]);L([(8,9),(9,9)]);L([(2,9),(4,9)])
        elif n=='desk':s.rr(cx-10,cy-8,cx+10,cy+4,2,outline=col,w=2.2);L([(-5,9),(5,9)]);L([(0,4),(0,9)])
        elif n=='mouse':s.rr(cx-6,cy-10,cx+6,cy+10,6,outline=col,w=2.2);L([(0,-5),(0,-1)])
        elif n=='dpad':L([(-3,-10),(3,-10),(3,-3),(10,-3),(10,3),(3,3),(3,10),(-3,10),(-3,3),(-10,3),(-10,-3),(-3,-3),(-3,-10)])
        elif n=='gear':
            s.circ(cx,cy,3,outline=col,w=2.2)
            for a in range(0,360,45):
                r=math.radians(a);L([(7*math.cos(r),7*math.sin(r)),(10.5*math.cos(r),10.5*math.sin(r))])
        elif n=='go':L([(-9,0),(9,0)]);L([(3,-6),(9,0),(3,6)])
        elif n=='up':L([(-6,3),(0,-3),(6,3)])
        elif n=='dn':L([(-6,-3),(0,3),(6,-3)])
        elif n=='vol':L([(-9,-3),(-5,-3),(0,-8),(0,8),(-5,3),(-9,3),(-9,-3)])
        elif n=='volup':s.icon('vol',cx-3,cy,col);L([(5,-4),(8,0),(5,4)])
        elif n=='mute':s.icon('vol',cx-3,cy,col);L([(4,-4),(10,4)]);L([(10,-4),(4,4)])
    def cursor(s,x,y,k=1.0):
        pts=[(0,0),(0,34),(9,26),(16,40),(22,37),(15,24),(27,24)]
        s.d.polygon([((x+px*k)*S,(y+py*k)*S) for px,py in pts],fill=(255,255,255),outline=(0,0,0),width=int(1.5*S))

def main_screen(L,lang):
    c=Cv(1280,720)
    c.rr(36,26,1244,100,37,fill=SURF,outline=LINE)
    def btn(n,cx,prim=False):
        c.circ(cx,63,26,fill=ACC if prim else SURF2); c.icon(n,cx,63,ONACC if prim else TEXT)
    for n,cx in (('back',74),('fwd',134),('reload',194),('home',254)):btn(n,cx)
    for n,cx in (('go',792),('bm',852),('hist',912),('folder',972),('qr',1032),('desk',1092),('mouse',1152),('gear',1212)):btn(n,cx,n in('mouse','go'))
    c.rr(290,37,760,89,26,fill=BG,outline=LINE); c.tx(314,63,L['url_hint'],15,MUTED)
    c.tx(640,270,L['start_title'],38,TEXT,True,'mm')
    c.rr(380,320,900,380,30,fill=SURF,outline=LINE); c.tx(412,350,L['search_hint'],18,MUTED)
    for i,(ch,nm) in enumerate((('Y','YouTube'),('V','Vimeo'),('W','Wikipedia'),('+',L['add']))):
        x=300+i*190; add=ch=='+'
        c.rr(x,430,x+170,590,24,fill=SURF if not add else BG,outline=LINE if not add else GRAY,w=1.5)
        c.circ(x+85,490,32,fill=(36,48,73) if not add else None,outline=None if not add else GRAY,w=1.5)
        c.tx(x+85,490,ch,28,ACC if not add else MUTED,True,'mm'); c.tx(x+85,550,nm,17,TEXT if not add else MUTED,True,'mm')
    c.cursor(488,540,1.5)
    c.save('docs/images/%s/main.png'%lang)

def settings_screen(L,lang):
    c=Cv(1280,1000)
    c.circ(86,66,28,fill=SURF2); c.icon('back',86,66,TEXT); c.tx(130,66,L['settings'],34,TEXT,True)
    def card(x,y,w,title,rows):
        h=24+34+len(rows)*60+10; c.rr(x,y,x+w,y+h,24,fill=SURF,outline=LINE); c.tx(x+26,y+36,title,20,TEXT,True)
        for i,(lab,kind,arg) in enumerate(rows):
            ry=y+62+i*60; c.d.line([((x+26)*S,ry*S),((x+w-26)*S,ry*S)],fill=LINE,width=S)
            c.tx(x+26,ry+30,lab,16,TEXT); rx=x+w-26
            if kind=='slider':
                tw=170;c.rr(rx-tw,ry+27,rx,ry+33,3,fill=SURF2);c.rr(rx-tw,ry+27,rx-tw+tw*arg,ry+33,3,fill=ACC);c.circ(rx-tw+tw*arg,ry+30,9,fill=ACC)
            elif kind=='slidertxt':
                tw=140;v,frac=arg;c.tx(rx-tw-14,ry+30,v,15,MUTED,False,'rm')
                c.rr(rx-tw,ry+27,rx,ry+33,3,fill=SURF2);c.rr(rx-tw,ry+27,rx-tw+tw*frac,ry+33,3,fill=ACC);c.circ(rx-tw+tw*frac,ry+30,9,fill=ACC)
            elif kind=='seg':
                opts,sel=arg;ws=[len(o)*9+44 for o in opts];tot=sum(ws)+8;x0=rx-tot
                c.rr(x0,ry+8,rx,ry+52,22,fill=BG);cx=x0+4
                for i2,o in enumerate(opts):
                    if i2==sel:c.rr(cx,ry+12,cx+ws[i2],ry+48,18,fill=ACC)
                    c.tx(cx+ws[i2]/2,ry+30,o,14,ONACC if i2==sel else MUTED,True,'mm');cx+=ws[i2]
            elif kind=='sw':
                for i2,col in enumerate(((255,255,255),(255,213,74),(110,231,168),(255,138,122))):
                    cx=rx-18-(3-i2)*40;c.circ(cx,ry+30,14,fill=col,outline=ACC if i2==0 else None,w=2.5)
            elif kind=='btn':
                tw=len(arg)*9+52;prim=arg in (L['update'],L['open']);c.rr(rx-tw,ry+8,rx,ry+52,22,fill=ACC if prim else SURF2);c.tx(rx-tw/2,ry+30,arg,14,ONACC if prim else TEXT,True,'mm')
            elif kind=='txt':c.tx(rx,ry+30,arg,15,MUTED,False,'rm')
        return h
    cw=560;lx,rx_=50,50+cw+24
    h1=card(lx,120,cw,L['mouse'],[(L['cursor_speed'],'slider',.6),(L['acceleration'],'slider',.4),(L['cursor_size'],'seg',(['S','M','L'],1)),(L['cursor_color'],'sw',0)])
    h2=card(lx,120+h1+20,cw,L['control'],[(L['mode'],'seg',([L['mouse'],L['buttons']],0)),(L['scroll_speed'],'slider',.5),(L['language'],'seg',([L['lang_auto'],'English','ქართული'],0))])
    card(lx,120+h1+20+h2+20,cw,L['update'],[(L['version'],'txt','2.31'),(L['update_link'],'txt','github.com/…/app-debug.apk'),(L['new_version'],'btn',L['update'])])
    h4=card(rx_,120,cw,L['browser'],[(L['site_version'],'seg',(['Desktop','Tablet','Phone'],0)),(L['text_scale'],'seg',(['75%','100%','125%','150%'],1)),(L['overscan'],'slidertxt',('2.0%',.2)),(L['home_page'],'txt','google.com'),(L['autohide'],'seg',([L['off'],L['on']],1))])
    card(rx_,120+h4+20,cw,L['data'],[(L['history'],'btn',L['clear']),(L['cookies_logins'],'btn',L['clear']),(L['sync'],'btn',L['open'])])
    c.save('docs/images/%s/settings.png'%lang)

def phone_screen(L,lang,buttons):
    c=Cv(390,844)
    c.circ(28,40,5,fill=(110,231,168));c.tx(44,40,L['remote_connected'],14,TEXT,True)
    c.rr(250,22,374,58,18,fill=SURF)
    c.rr(254,26,306,54,14,fill=SURF if buttons else ACC);c.icon('mouse',280,40,MUTED if buttons else ONACC)
    c.rr(318,26,370,54,14,fill=ACC if buttons else SURF);c.icon('dpad',344,40,ONACC if buttons else MUTED)
    if not buttons:
        c.rr(16,72,374,590,36,fill=SURF,outline=LINE);c.circ(180,331,28,fill=SURF2)
        c.rr(316,92,362,570,23,fill=SURF2);c.icon('up',339,114,MUTED);c.icon('dn',339,548,MUTED);c.rr(337,150,341,512,2,fill=GRAY)
    else:
        for i in range(8):
            x=16+(i%4)*92;y=72+(i//4)*62
            c.rr(x,y,x+84,y+52,26,fill=SURF2);c.icon(('fwd','reload','bm','hist','folder','back','gear','desk')[i],x+42,y+26,TEXT)
        c.circ(195,400,170,fill=SURF,outline=LINE);c.circ(195,400,66,fill=ACC);c.tx(195,400,'OK',20,ONACC,True,'mm')
        c.icon('up',195,262,TEXT);c.icon('dn',195,538,TEXT);c.icon('back',58,400,TEXT);c.icon('fwd',332,400,TEXT)
    c.circ(78,650,31,fill=SURF2);c.icon('back',78,650,TEXT)
    c.circ(195,650,40,fill=SURF2);c.icon('home',195,650,TEXT)
    c.circ(312,650,31,fill=SURF2);c.icon('mute',312,650,TEXT)
    c.rr(16,716,374,772,28,fill=SURF2);c.d.line([(195*S,724*S),(195*S,764*S)],fill=GRAY,width=S)
    c.icon('vol',105,744,TEXT);c.icon('volup',285,744,TEXT)
    c.save('docs/images/%s/%s.png'%(lang,'phone-buttons' if buttons else 'phone'))

langs={'en':RES+'values/strings.xml'}
for p in sorted(glob.glob(RES+'values-*/strings.xml')): langs[p.split('values-')[1].split('/')[0]]=p
base=strings(langs['en'])
for lang,path in langs.items():
    L=dict(base); L.update(strings(path))
    main_screen(L,lang);settings_screen(L,lang);phone_screen(L,lang,False);phone_screen(L,lang,True)
print('previews:',', '.join(langs))
