package com.example.tvbrowser

object RemotePage {
    const val HTML = """<!doctype html>
<html lang="ka"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<title>TV პულტი</title>
<style>
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
html,body{margin:0;height:100%;overflow:hidden;overscroll-behavior:none;background:#0E1116;color:#F2F4F8;font-family:system-ui,'Noto Sans Georgian',sans-serif;touch-action:none}
body{display:flex;flex-direction:column;gap:12px;padding:14px 16px 16px}
button{font-family:inherit;border:0;color:#F2F4F8;background:#222833;display:flex;align-items:center;justify-content:center;padding:0}
.i{width:26px;height:26px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
header{display:flex;align-items:center;justify-content:space-between;height:40px}
.st{display:flex;align-items:center;gap:10px;font-weight:600;font-size:15px}
#dot{width:10px;height:10px;border-radius:5px;background:#FF8A7A}#dot.ok{background:#6EE7A8}
.seg{display:flex;background:#171B22;border-radius:22px;padding:4px;gap:2px}
.seg button{height:34px;width:46px;border-radius:17px;background:transparent;color:#9AA4B2}
.seg .on{background:#6EA8FF;color:#0B1220}
#pad{flex:1;position:relative;border-radius:36px;background:#171B22;border:1px solid #262C36;min-height:0}
#pad:after{content:'';position:absolute;left:50%;top:50%;width:56px;height:56px;margin:-28px;border-radius:50%;background:#222833;pointer-events:none}
#scr{position:absolute;left:0;top:0;bottom:0;width:44px;display:flex;align-items:center}
#scr i{display:block;width:5px;height:96px;margin-left:10px;border-radius:3px;background:#3A4352}
#nav{display:none;grid-template-columns:repeat(4,1fr);gap:10px}
#nav button{height:52px;border-radius:26px}
#dw{display:none;flex:1;align-items:center;justify-content:center;min-height:0}
#dpad{height:100%;max-width:100%;aspect-ratio:1/1;border-radius:50%;background:#171B22;border:1px solid #262C36;position:relative}
#dpad button{position:absolute;background:transparent;width:22%;height:22%;border-radius:50%}
#dpad .u{top:5%;left:39%}#dpad .dn{bottom:5%;left:39%}#dpad .l{left:5%;top:39%}#dpad .r{right:5%;top:39%}
#dpad .ok{left:50%;top:50%;width:40%;height:40%;transform:translate(-50%,-50%);background:#6EA8FF;color:#0B1220;font-weight:700;font-size:18px}
.r1{display:flex;align-items:center;justify-content:space-between}
.r1 button{width:62px;height:62px;border-radius:50%}
.r1 .home{width:80px;height:80px}
.r1 .acc{background:#6EA8FF;color:#0B1220}
.r2{display:flex;gap:10px}
.r2 button{height:56px}
.r2 .mute{width:56px;border-radius:50%}
.vol{flex:1;display:flex;border-radius:28px;overflow:hidden;background:#222833}
.vol button{flex:1;border-radius:0;background:transparent}
.vol button+button{border-left:1px solid #3A4352}
body.m1 #pad{display:none}
body.m1 #nav{display:grid}
body.m1 #dw{display:flex}
.tx{display:flex;gap:10px}
.tx input{flex:1;min-width:0;height:50px;border-radius:25px;border:1px solid #262C36;background:#171B22;color:#F2F4F8;padding:0 20px;font-size:16px;touch-action:auto;font-family:inherit}
.tx button{height:50px;padding:0 20px;border-radius:25px;background:#6EA8FF;color:#0B1220;font-weight:700;font-size:15px}
</style></head><body>
<svg width="0" height="0" style="position:absolute"><defs>
<symbol id="back" viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6"/></symbol>
<symbol id="fwd" viewBox="0 0 24 24"><path d="M9 6l6 6-6 6"/></symbol>
<symbol id="reload" viewBox="0 0 24 24"><path d="M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5"/></symbol>
<symbol id="home" viewBox="0 0 24 24"><path d="M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10"/></symbol>
<symbol id="bm" viewBox="0 0 24 24"><path d="M7 4h10v16l-5-4-5 4z"/></symbol>
<symbol id="hist" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></symbol>
<symbol id="files" viewBox="0 0 24 24"><path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></symbol>
<symbol id="url" viewBox="0 0 24 24"><path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/></symbol>
<symbol id="set" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2"/></symbol>
<symbol id="desk" viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="11" rx="2"/><path d="M8 20h8M12 16v4"/></symbol>
<symbol id="full" viewBox="0 0 24 24"><path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/></symbol>
<symbol id="mouse" viewBox="0 0 24 24"><rect x="7" y="3" width="10" height="18" rx="5"/><path d="M12 7v4"/></symbol>
<symbol id="dp" viewBox="0 0 24 24"><path d="M9 3h6v6h6v6h-6v6H9v-6H3V9h6z"/></symbol>
<symbol id="up" viewBox="0 0 24 24"><path d="M6 15l6-6 6 6"/></symbol>
<symbol id="dn" viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></symbol>
<symbol id="vm" viewBox="0 0 24 24"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M17 9l4 6M21 9l-4 6"/></symbol>
<symbol id="vd" viewBox="0 0 24 24"><path d="M4 9v6h4l5 4V5L8 9z"/></symbol>
<symbol id="vu" viewBox="0 0 24 24"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M16.5 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12"/></symbol>
</defs></svg>
<header><div class="st"><span id="dot"></span><span id="stx">კავშირი…</span></div>
<div class="seg"><button id="m0" class="on" aria-label="მაუსი"><svg class="i"><use href="#mouse"/></svg></button><button id="m1" aria-label="ღილაკები"><svg class="i"><use href="#dp"/></svg></button></div></header>
<div id="pad"><div id="scr"><i></i></div></div>
<div id="nav">
<button data-a="fwd" aria-label="წინ"><svg class="i"><use href="#fwd"/></svg></button>
<button data-a="reload" aria-label="განახლება"><svg class="i"><use href="#reload"/></svg></button>
<button data-a="bm" aria-label="სანიშნეები"><svg class="i"><use href="#bm"/></svg></button>
<button data-a="hist" aria-label="ისტორია"><svg class="i"><use href="#hist"/></svg></button>
<button data-a="files" aria-label="ფაილები"><svg class="i"><use href="#files"/></svg></button>
<button data-a="url" aria-label="მისამართი"><svg class="i"><use href="#url"/></svg></button>
<button data-a="set" aria-label="პარამეტრები"><svg class="i"><use href="#set"/></svg></button>
<button data-a="ua" aria-label="საიტის ვერსია"><svg class="i"><use href="#desk"/></svg></button>
</div>
<div id="dw"><div id="dpad">
<button class="u" data-d="up" aria-label="ზევით"><svg class="i"><use href="#up"/></svg></button>
<button class="dn" data-d="dn" aria-label="ქვევით"><svg class="i"><use href="#dn"/></svg></button>
<button class="l" data-d="l" aria-label="მარცხნივ"><svg class="i"><use href="#back"/></svg></button>
<button class="r" data-d="r" aria-label="მარჯვნივ"><svg class="i"><use href="#fwd"/></svg></button>
<button class="ok" data-k="ok">OK</button>
</div></div>
<div class="r1">
<button data-k="back" aria-label="უკან"><svg class="i"><use href="#back"/></svg></button>
<button class="home" data-a="home" aria-label="მთავარი"><svg class="i"><use href="#home"/></svg></button>
<button class="acc" data-k="full" aria-label="სრული ეკრანი"><svg class="i"><use href="#full"/></svg></button>
</div>
<div class="r2">
<button class="mute" data-v="mute" aria-label="ხმის გამორთვა"><svg class="i"><use href="#vm"/></svg></button>
<div class="vol"><button data-v="dn" aria-label="ხმა −"><svg class="i"><use href="#vd"/></svg></button><button data-v="up" aria-label="ხმა +"><svg class="i"><use href="#vu"/></svg></button></div>
</div>
<div class="tx"><input id="t" placeholder="ტექსტის აკრეფა…" autocomplete="off" autocapitalize="off"><button id="send">გაგზავნა</button></div>
<script>
var g=function(i){return document.getElementById(i)};
var K=new URLSearchParams(location.search).get('k'),ws=null,mode=0;
function conn(){
 ws=new WebSocket('ws://'+location.host+'/ws?k='+K);
 ws.onopen=function(){g('dot').className='ok';g('stx').textContent='დაკავშირებულია';send('mode '+mode);};
 ws.onclose=function(){g('dot').className='';g('stx').textContent='კავშირი…';setTimeout(conn,1000);};
}
function send(s){if(ws&&ws.readyState==1)ws.send(s);}
function setMode(m){mode=m;g('m0').className=m==0?'on':'';g('m1').className=m==1?'on':'';
 document.body.className=m==1?'m1':'';send('mode '+m);}
g('m0').onclick=function(){setMode(0)};g('m1').onclick=function(){setMode(1)};
var pad=g('pad'),lx=0,ly=0,sx=0,sy=0,t0=0,moved=0,ax=0,ay=0,tm=null;
function flush(){if(tm)return;tm=setTimeout(function(){tm=null;if(mode==0&&(ax||ay))send('m '+Math.round(ax)+' '+Math.round(ay));ax=0;ay=0;},25);}
pad.addEventListener('touchstart',function(e){if(e.target.closest('#scr'))return;var t=e.touches[0];lx=sx=t.clientX;ly=sy=t.clientY;t0=Date.now();moved=0;e.preventDefault();},{passive:false});
pad.addEventListener('touchmove',function(e){if(e.target.closest('#scr'))return;var t=e.touches[0];ax+=t.clientX-lx;ay+=t.clientY-ly;lx=t.clientX;ly=t.clientY;moved=Math.max(moved,Math.abs(t.clientX-sx)+Math.abs(t.clientY-sy));e.preventDefault();flush();},{passive:false});
pad.addEventListener('touchend',function(e){if(e.target.closest('#scr'))return;if(moved<10&&Date.now()-t0<350){send('c');}});
var sc=g('scr'),sy0=0;
sc.addEventListener('touchstart',function(e){sy0=e.touches[0].clientY;e.preventDefault();},{passive:false});
sc.addEventListener('touchmove',function(e){var y=e.touches[0].clientY;send('s '+Math.round(sy0-y));sy0=y;e.preventDefault();},{passive:false});
function bind(sel,attr,cmd){document.querySelectorAll(sel).forEach(function(b){b.onclick=function(){send(cmd+' '+b.getAttribute(attr));};});}
bind('[data-k]','data-k','k');bind('[data-a]','data-a','a');bind('[data-d]','data-d','d');bind('[data-v]','data-v','v');
var ti=g('t');
function sendText(){if(ti.value){send('t '+ti.value);ti.value='';}}
g('send').onclick=sendText;
ti.addEventListener('keydown',function(e){
 if(e.key=='Enter'){sendText();send('e');e.preventDefault();}
 else if(e.key=='Backspace'&&!ti.value){send('b');}
});
conn();
</script></body></html>"""
}
