package com.example.tvbrowser

object RemotePage {
    const val HTML = """<!doctype html>
<html lang="ka"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<title>TV პულტი</title>
<style>
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
html,body{margin:0;height:100%;overflow:hidden;overscroll-behavior:none;background:#0E1116;color:#F2F4F8;font-family:system-ui,'Noto Sans Georgian',sans-serif;touch-action:none}
body{display:flex;flex-direction:column;gap:12px;padding:16px}
header{display:flex;align-items:center;justify-content:space-between;height:44px}
.st{display:flex;align-items:center;gap:10px;font-weight:600}
#dot{width:10px;height:10px;border-radius:5px;background:#FF8A7A}#dot.ok{background:#6EE7A8}
.seg{display:flex;background:#171B22;border-radius:22px;padding:4px;gap:2px}
.seg button{height:36px;padding:0 14px;border-radius:18px;border:0;background:transparent;color:#9AA4B2;font-weight:600;font-size:14px;font-family:inherit}
.seg .on{background:#6EA8FF;color:#0B1220}
button{font-family:inherit}
#pad{flex:1;position:relative;border-radius:28px;background:#171B22;border:1px solid #262C36;display:flex;align-items:center;justify-content:center;text-align:center;color:#9AA4B2;line-height:1.6;padding:0 70px;font-size:15px}
#scr{position:absolute;right:12px;top:16px;bottom:16px;width:44px;border-radius:22px;background:#222833;display:flex;flex-direction:column;align-items:center;justify-content:space-between;padding:12px 0;color:#F2F4F8;font-size:12px}
.row{display:flex;gap:10px}
.row button{flex:1;height:64px;border-radius:22px;border:0;background:#222833;color:#F2F4F8;font-weight:600;font-size:13px}
.row .acc{background:#6EA8FF;color:#0B1220;font-size:16px;font-weight:700}
#nav{display:none;grid-template-columns:repeat(3,1fr);gap:10px}
#nav button{height:54px;border-radius:18px;border:0;background:#222833;color:#F2F4F8;font-weight:600;font-size:13px}
#dpad{display:none;flex:1;grid-template-columns:repeat(3,1fr);grid-template-rows:repeat(3,1fr);gap:10px;min-height:0}
#dpad button{border-radius:22px;border:0;background:#222833;color:#F2F4F8;font-size:22px}
#dpad .acc{background:#6EA8FF;color:#0B1220;font-weight:700;font-size:18px}
#dpad .e{background:transparent;pointer-events:none}
body.m1 #pad,body.m1 .row{display:none}
body.m1 #nav{display:grid}
body.m1 #dpad{display:grid}
.tx{display:flex;gap:10px}
.tx input{flex:1;min-width:0;height:54px;border-radius:27px;border:1px solid #262C36;background:#171B22;color:#F2F4F8;padding:0 20px;font-size:16px;touch-action:auto;font-family:inherit}
.tx button{height:54px;padding:0 22px;border-radius:27px;border:0;background:#6EA8FF;color:#0B1220;font-weight:700;font-size:16px}
</style></head><body>
<header><div class="st"><span id="dot"></span><span id="stx">კავშირი…</span></div>
<div class="seg"><button id="m0" class="on">მაუსი</button><button id="m1">ღილაკები</button></div></header>
<div id="pad"><div id="hint">გადაათრიე კურსორის გასაადგილებლად<br>შეხება = კლიკი</div>
<div id="scr"><span>▲</span><span>გადახვევა</span><span>▼</span></div></div>
<div id="nav">
<button data-a="back">◀ უკან</button><button data-a="fwd">წინ ▶</button><button data-a="reload">განახლება</button>
<button data-a="home">მთავარი</button><button data-a="bm">სანიშნეები</button><button data-a="hist">ისტორია</button>
<button data-a="url">მისამართი</button><button data-a="set">პარამეტრები</button><button data-a="full">სრული ეკრანი</button>
</div>
<div id="dpad">
<span class="e"></span><button data-d="up">▲</button><span class="e"></span>
<button data-d="l">◀</button><button data-k="ok" class="acc">OK</button><button data-d="r">▶</button>
<span class="e"></span><button data-d="dn">▼</button><span class="e"></span>
</div>
<div class="row"><button data-k="back">უკან</button><button data-k="reload">განახლება</button><button data-k="full">სრული ეკრანი</button><button data-k="ok" class="acc">OK</button></div>
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
pad.addEventListener('touchend',function(e){if(e.target.closest('#scr'))return;
 if(moved<10&&Date.now()-t0<350){send('c');}
});
var sc=g('scr'),sy0=0;
sc.addEventListener('touchstart',function(e){sy0=e.touches[0].clientY;e.preventDefault();},{passive:false});
sc.addEventListener('touchmove',function(e){var y=e.touches[0].clientY;send('s '+Math.round(sy0-y));sy0=y;e.preventDefault();},{passive:false});
function bind(sel,attr,cmd){document.querySelectorAll(sel).forEach(function(b){b.onclick=function(){send(cmd+' '+b.getAttribute(attr));};});}
bind('[data-k]','data-k','k');bind('[data-a]','data-a','a');bind('[data-d]','data-d','d');
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
