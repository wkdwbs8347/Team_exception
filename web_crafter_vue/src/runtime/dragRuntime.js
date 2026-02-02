// src/runtime/dragRuntime.js
export const DRAG_RUNTIME_JS = `
(function(){
  const isRunning = !!window.__WC_IS_RUNNING__;
  if(isRunning) return;

  const wrap = document.getElementById("wrapper");
  if(!wrap) return;

  let dragging=null;

  wrap.addEventListener("pointerdown",(ev)=>{
    if(isRunning) return;
    const t = ev.target.closest("#wrapper > [data-draggable='true'][data-block-id]");
    if(!t) return;

    window.__WC_DRAGGING__ = true;

    const baseLeft = t.offsetLeft;
    const baseTop = t.offsetTop;

    const wrapW = wrap.clientWidth || 0;
    const wrapH = wrap.scrollHeight || wrap.clientHeight || 0;
    const elW = t.offsetWidth || 0;
    const elH = t.offsetHeight || 0;

    dragging={
      el:t,
      baseLeft,
      baseTop,
      startX:ev.clientX,
      startY:ev.clientY,
      guides:(window.collectGuides ? window.collectGuides(t) : null),
      pointerId:ev.pointerId,
      bounds:{ wrapW, wrapH, elW, elH }
    };

    t.classList.add("wc-dragging");
    try{ t.setPointerCapture(ev.pointerId); }catch(e){}
    window.parent.postMessage({type:"select_block",blockId:t.getAttribute("data-block-id")},"*");
  });

  wrap.addEventListener("pointermove",(ev)=>{
    ev.preventDefault();
    if(!dragging) return;

    const dx=ev.clientX-dragging.startX;
    const dy=ev.clientY-dragging.startY;
    let nextL=dragging.baseLeft+dx;
    let nextT=dragging.baseTop+dy;

    const b = dragging.bounds || {wrapW:0, wrapH:0, elW:0, elH:0};
    const maxL = Math.max(0, (b.wrapW||0) - (b.elW||0));

    if(nextL<0)nextL=0;
    if(nextT<0)nextT=0;
    if(nextL>maxL)nextL=maxL;

    const snap = (window.computeSmartSnap && window.collectGuides)
      ? window.computeSmartSnap({
          nextLeft: nextL,
          nextTop: nextT,
          width: (b.elW||0),
          height: (b.elH||0),
          guides: dragging.guides
        })
      : {dx:0, dy:0};

    if(window.hideGuides) window.hideGuides();
    if(snap.vLine && window.showVSeg) window.showVSeg(snap.vLine,snap.vSeg.y1,snap.vSeg.y2);
    if(snap.hLine && window.showHSeg) window.showHSeg(snap.hLine,snap.hSeg.x1,snap.hSeg.x2);

    let finalL = nextL + (snap.dx||0);
    let finalT = nextT + (snap.dy||0);
    if(finalL<0)finalL=0;
    if(finalT<0)finalT=0;
    if(finalL>maxL)finalL=maxL;

    const edge = 40; const speed = 18;
    if(ev.clientY > window.innerHeight - edge) window.scrollBy(0, speed);
    else if(ev.clientY < edge) window.scrollBy(0, -speed);

    dragging.el.style.setProperty("left", finalL+"px", "important");
    dragging.el.style.setProperty("top", finalT+"px", "important");
  });

  wrap.addEventListener("pointerup",(ev)=>{
    if(!dragging) return;
    const t=dragging.el;

    if(window.hideGuides) window.hideGuides();
    t.classList.remove("wc-dragging");
    window.__WC_DRAGGING__ = false;

    try{ t.releasePointerCapture(dragging.pointerId); }catch(e){}

    const finalX = Math.round(parseFloat(t.style.left || "0"));
    const finalY = Math.round(parseFloat(t.style.top || "0"));

    window.parent.postMessage({
      type:"update_free_position",
      blockId:t.getAttribute("data-block-id"),
      x: finalX,
      y: finalY
    },"*");

    if(window.updateWrapperHeight) setTimeout(window.updateWrapperHeight, 0);
    dragging=null;
  });
})();
`;