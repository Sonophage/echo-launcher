// Canvas ports of ECHO's boot ripple (BootRipple.kt), launch disc (DiscLaunchCeremony.kt) and GameBoot (GameBootSequence.kt).
(function () {
  const progress = (t, a, b) => Math.min(1, Math.max(0, (t - a) / (b - a)));
  const easeOut = x => 1 - Math.pow(1 - x, 3);
  const easeInOut = x => x < .5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
  const mix = (a, b, x) => a + (b - a) * x;
  const bez = (x1, y1, x2, y2) => t => { // cubic-bezier easing
    const cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx, cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by;
    let u = t; for (let i = 0; i < 6; i++) { const x = ((ax * u + bx) * u + cx) * u - t, d = (3 * ax * u + 2 * bx) * u + cx; if (Math.abs(d) < 1e-6) break; u -= x / d; }
    return ((ay * u + by) * u + cy) * u;
  };
  const FOS = bez(.4, 0, .2, 1), LOS = bez(0, 0, .2, 1);
  const P = {
    l: new Path2D('M66.889,18.685 A26,26 0 1,0 66.889,61.315 A30,30 0 0,1 66.889,18.685 Z'),
    r: new Path2D('M109.111,18.685 A26,26 0 1,1 109.111,61.315 A30,30 0 0,0 109.111,18.685 Z'),
    ring: new Path2D('M67,40 a21,21 0 1,0 42,0 a21,21 0 1,0 -42,0'),
    dot: new Path2D('M80,86 a8,8 0 1,0 16,0 a8,8 0 1,0 -16,0'),
  };
  function drawMark(c, p) {
    const k = p.size * p.ms / 132;
    c.save(); c.fillStyle = c.strokeStyle = '#fff';
    c.translate(p.cx - 66 * k, p.cy - 66 * k); c.scale(k, k); c.translate(-22, 12);
    const al = p.alpha ?? 1, base = c.globalAlpha; if (al <= 0) { c.restore(); return; }
    const sx = p.ringScaleX ?? p.ringScale ?? 1, sy = p.ringScaleY ?? sx;
    c.globalAlpha = base * al * p.crescentAlpha;
    c.save(); c.translate(p.lx, 0); c.fill(P.l); c.restore();
    c.save(); c.translate(p.rx, 0); c.fill(P.r); c.restore();
    if (p.ringAlpha > 0) { c.save(); c.globalAlpha = base * al * p.ringAlpha; c.translate(88, 40); c.scale(sx, sy); c.rotate(Math.PI / 2); c.translate(-88, -40); c.lineWidth = 5; if (p.ringOffset > 0) { c.setLineDash([132, 132]); c.lineDashOffset = p.ringOffset; } c.stroke(P.ring); c.restore(); }
    c.save(); c.globalAlpha = base * al * p.dotAlpha; c.translate(0, p.dotY || 0); c.translate(88, 86); c.scale(p.dotScale ?? 1, p.dotScale ?? 1); c.translate(-88, -86); c.fill(P.dot); c.restore();
    c.restore();
  }
  function echoRing(t, start, dur, x, y, r0, r1, op) {
    const w = progress(t, start, start + dur); if (w <= 0 || w >= 1) return null;
    return { x, y, r: mix(r0, r1, easeOut(w)), sw: 1.6 * (1 - w) + .4, a: op * (1 - w) };
  }
  function bootFrame(c, t, w, h) {
    const size = .42 * h, k = size / 132, cx = w / 2, cy = h / 2, ry = cy - 14 * k;
    const g = easeOut(progress(t, 0, 500)), cc = easeInOut(progress(t, 500, 850));
    const line = t < 500 ? .7 * w * g : mix(.7 * w, 0, cc);
    const a = easeOut(progress(t, 700, 1000)), ring = easeOut(progress(t, 1150, 1650)), cres = easeOut(progress(t, 1450, 2100));
    const word = easeOut(progress(t, 1900, 2500));
    let ms = 1 + .012 * Math.sin(progress(t, 2500, 2900) * Math.PI);
    const exit = easeInOut(progress(t, 2900, 3500)); ms *= 1 + .04 * exit;
    c.clearRect(0, 0, w, h);
    c.save(); c.globalAlpha = 1 - exit;
    c.fillStyle = '#04060C'; c.fillRect(0, 0, w, h);
    const ga = a * (1 - .7 * ring), gr = .16 * h;
    if (ga > 0) { const rg = c.createRadialGradient(cx, ry, 0, cx, ry, gr); rg.addColorStop(0, 'rgba(255,255,255,.22)'); rg.addColorStop(1, 'rgba(255,255,255,0)'); c.save(); c.globalAlpha *= ga; c.fillStyle = rg; c.fillRect(cx - gr, ry - gr, gr * 2, gr * 2); c.restore(); }
    [850, 1100, 1350].forEach(s => { const r = echoRing(t, s, 1800, cx, ry, 4 * k, .62 * w, .42); if (!r) return; c.save(); c.globalAlpha *= r.a; c.strokeStyle = '#fff'; c.lineWidth = r.sw; c.beginPath(); c.arc(r.x, r.y, r.r, 0, Math.PI * 2); c.stroke(); c.restore(); });
    const la = 1 - progress(t, 780, 850);
    if (la > 0 && line > 0) { const lg = c.createLinearGradient(cx - line / 2, 0, cx + line / 2, 0); lg.addColorStop(0, 'rgba(255,255,255,0)'); lg.addColorStop(.3, '#fff'); lg.addColorStop(.7, '#fff'); lg.addColorStop(1, 'rgba(255,255,255,0)'); c.save(); c.globalAlpha *= la; c.fillStyle = lg; c.fillRect(cx - line / 2, ry - .75, line, 1.5); c.restore(); }
    c.save(); c.globalAlpha *= 1;
    const base = c.globalAlpha;
    const pose = { cx, cy, size, ms, lx: -24 * (1 - cres), rx: 24 * (1 - cres), crescentAlpha: base * easeOut(progress(t, 1450, 1950)), ringAlpha: base * ring, ringScale: mix(.85, 1, ring), dotY: -46 + 46 * easeInOut(progress(t, 1150, 1650)), dotScale: mix(.2, 1, a), dotAlpha: base * a };
    drawMark(c, pose); c.restore();
    if (word > 0) { const fs = Math.max(11, h * .018); c.save(); c.globalAlpha *= .6 * word; c.fillStyle = '#fff'; c.font = `300 ${fs}px Sora, sans-serif`; c.textAlign = 'center'; c.letterSpacing = `${mix(10, 4, word) * fs / 13}px`; c.fillText('ECHO', w / 2 + mix(10, 4, word) * fs / 26, h - h * .066); c.restore(); }
    c.restore();
  }

  function setupCanvas(el, cv) {
    const fit = () => { const r = el.getBoundingClientRect(), d = Math.min(2, window.devicePixelRatio || 1); cv.width = Math.max(1, r.width * d); cv.height = Math.max(1, r.height * d); cv._d = d; cv._w = r.width; cv._h = r.height; };
    fit(); const ro = new ResizeObserver(fit); ro.observe(el); return ro;
  }

  class EchoBoot extends HTMLElement {
    connectedCallback() {
      if (this._init) return; this._init = true;
      const overlay = this.hasAttribute('overlay');
      Object.assign(this.style, overlay ? { position: 'fixed', inset: '0', zIndex: '1000', display: 'block', cursor: 'pointer' } : { position: 'relative', display: 'block', width: '100%', height: '100%', cursor: 'pointer', background: this.getAttribute('bg') ? `#04060C url("${this.getAttribute('bg')}") center/cover` : '#04060C' });
      this.cv = document.createElement('canvas'); Object.assign(this.cv.style, { position: 'absolute', inset: '0', width: '100%', height: '100%', display: 'block' });
      this.appendChild(this.cv); this.ro = setupCanvas(this, this.cv);
      if (!overlay) {
        this.hint = document.createElement('div'); this.hint.textContent = 'Replay';
        Object.assign(this.hint.style, { position: 'absolute', right: '16px', bottom: '14px', font: '500 12px Sora, sans-serif', letterSpacing: '.16em', textTransform: 'uppercase', color: '#fff', padding: '8px 14px', borderRadius: '999px', background: 'rgba(4,6,12,.55)', border: '1px solid rgba(255,255,255,.25)', backdropFilter: 'blur(8px)', opacity: '0', transition: 'opacity .4s' });
        this.appendChild(this.hint);
        this.addEventListener('click', () => this.play());
        this.io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting && !this._played) { this._played = true; this.play(); } }), { threshold: .5 });
        this.io.observe(this);
        const c = this.cv.getContext('2d'); c.setTransform(this.cv._d, 0, 0, this.cv._d, 0, 0); c.fillStyle = '#04060C'; c.fillRect(0, 0, this.cv._w, this.cv._h);
      } else {
        const skip = () => { if (this._skipped) return; this._skipped = true; this.style.transition = 'opacity .3s'; this.style.opacity = '0'; setTimeout(() => { cancelAnimationFrame(this.raf); this.done(); }, 300); };
        this.addEventListener('click', skip); this._key = e => { if (['Enter', ' ', 'Escape', 'a', 'b'].includes(e.key)) skip(); }; window.addEventListener('keydown', this._key);
        document.documentElement.style.overflow = 'hidden';
        requestAnimationFrame(() => this.play());
      }
    }
    disconnectedCallback() { this.ro && this.ro.disconnect(); this.io && this.io.disconnect(); cancelAnimationFrame(this.raf); this._key && window.removeEventListener('keydown', this._key); }
    play() {
      cancelAnimationFrame(this.raf); this.t0 = performance.now(); this.skipAt = null; if (this.hint) this.hint.style.opacity = '0';
      const tick = now => {
        const t = now - this.t0;
        const c = this.cv.getContext('2d'); c.setTransform(this.cv._d, 0, 0, this.cv._d, 0, 0);
        bootFrame(c, Math.min(t, 3500), this.cv._w, this.cv._h);
        if (t < 3500) this.raf = requestAnimationFrame(tick); else this.done();
      };
      this.raf = requestAnimationFrame(tick);
    }
    done() {
      if (this.hasAttribute('overlay')) { document.documentElement.style.overflow = ''; this.style.display = 'none'; window.removeEventListener('keydown', this._key); }
      else if (this.hint) this.hint.style.opacity = '1';
    }
  }

  // DiscCeremony constants
  const D = { FadeIn: 1700, Sink: 1150, Spin: 3350, Out: 650, Hold: 900 };
  D.Total = D.FadeIn + D.Sink + D.Spin + D.Out + D.Hold; D.OutStart = D.FadeIn + D.Sink + D.Spin; D.HandOff = D.OutStart + D.Out;
  const at = ms => ms / D.Total, ph = (n, a, b) => b <= a ? 1 : Math.min(1, Math.max(0, (n - a) / (b - a)));
  const FIF = at(D.FadeIn), SEF = at(D.FadeIn + D.Sink), DOF = at(D.OutStart), ROF = at(D.HandOff) + (1 - at(D.HandOff)) * .3;
  const LOUD = [[0, 0], [50, .11], [100, .37], [150, .82], [200, 1], [250, .6], [300, .37], [350, .22], [400, .14], [450, .06], [500, .03], [650, .01], [2000, 0]];
  const loud = ms => { for (let i = 1; i < LOUD.length; i++) { if (ms > LOUD[i][0]) continue; const [a, av] = LOUD[i - 1], [b, bv] = LOUD[i]; return av + (bv - av) * (ms - a) / (b - a); } return 0; };
  const smooth = (e0, e1, x) => { const t = Math.min(1, Math.max(0, (x - e0) / (e1 - e0))); return t * t * (3 - 2 * t); };

  // L2 Lens (LensLaunchCeremony.kt): the card shrinks into the ECHO ring, spins there as a disc, then the ring opens like a lens
  const LENS = { HAND_OFF: 3400, END: 5400, FADE: 500 };
  const spinAngle = t => { const v = 720 / 1500; if (t <= 700) return 0; if (t <= 1300) { const x = t - 700; return v * x * x / 1200; } if (t <= 2300) return v * 300 + v * (t - 1300); if (t <= 2700) { const x = t - 2300; return v * 1300 + v * (x - x * x / 800); } return 720; };
  const waitPulse = (t, t0) => t < t0 ? 1 : .4 + .6 * (.5 + .5 * Math.cos(2 * Math.PI * (t - t0) / 1200));
  function lensFrame(t, w, h) {
    const srcW = .202 * w, srcH = .193 * h, srcX = (w - srcW) / 2, srcY = (h - srcH) / 2;
    let size = .36 * h; const k = size / 132; let cy = h / 2 + 14 * k;
    const rcx = w / 2, rcy = h / 2, ir = 18.5 * k, rMax = Math.hypot(w / 2, h / 2) + 4;
    const a = easeInOut(progress(t, 0, 700)), d = easeInOut(progress(t, 2700, 3400)), r = mix(ir, rMax, d);
    const glowR = mix(.1 * h, .8 * h, easeOut(progress(t, 300, 1500))), cc = easeOut(progress(t, 600, 1400)), dp = progress(t, 900, 1400);
    const rs = r / ir * (20 / 21) + 1 / 21;
    let mark = { cx: w / 2, cy, size, ms: 1, alpha: 1, lx: -40 * (1 - cc) - 60 * d, rx: 40 * (1 - cc) + 60 * d, crescentAlpha: cc * (1 - d), ringAlpha: t < 700 ? 0 : 1 - progress(t, 2700, 3000), ringScaleX: rs, ringScaleY: rs, ringOffset: 132 * (1 - easeInOut(progress(t, 700, 1200))), dotY: 30 * (1 - easeOut(dp)), dotScale: 1, dotAlpha: easeOut(dp) * (1 - d) };
    const settled = easeOut(progress(t, 3600, 4100));
    if (t > 3400) mark = { cx: w / 2, cy: .66 * h, size: .12 * h, ms: 1, alpha: settled, lx: 0, rx: 0, crescentAlpha: 1, ringAlpha: 1, ringScaleX: 1, ringScaleY: 1, ringOffset: 0, dotY: 0, dotScale: 1, dotAlpha: waitPulse(t, 4100) };
    return {
      dim: .85 * easeOut(progress(t, 0, 600)),
      cardX: mix(srcX, rcx - ir, a), cardY: mix(srcY, rcy - ir, a), cardW: mix(srcW, 2 * ir, a), cardH: mix(srcH, 2 * ir, a), cardR: mix(8 * h / 462, ir, a), cardAlpha: 1 - progress(t, 600, 800),
      rx: rcx, ry: rcy, artR: r, artAlpha: progress(t, 600, 800), artDim: .4 * easeOut(progress(t, 3400, 4000)), artScale: mix(1.15, 1, d), artRot: spinAngle(t) % 360,
      discAlpha: progress(t, 600, 800) * (1 - progress(t, 2650, 2750)), discR: ir,
      glowR, glowAlpha: easeOut(progress(t, 300, 1200)) * (1 - d), waveAlpha: .6 * easeOut(progress(t, 1200, 2000)) * (1 - d),
      mark, titleY: .36 * h, titleAlpha: settled
    };
  }
  // the footer's action orb (EchoHintBar.kt): a glowing A at rest; holding raises a card joined to it, its ring filling over LAUNCH_HOLD_MS
  const HOLD_MS = 1000, ACCENT = [18, 139, 201];
  const lerpC = (c, to, x) => c.map((v, i) => Math.round(v + (to[i] - v) * x));
  const rgb = (c, a = 1) => `rgba(${c[0]},${c[1]},${c[2]},${a})`;
  const FILL = rgb(lerpC(ACCENT, [0, 0, 0], .35)), EDGE = lerpC(ACCENT, [255, 255, 255], .3);

  class EchoLaunch extends HTMLElement {
    static get observedAttributes() { return ['glyph', 'game-title', 'gametitle']; }
    title_() { return this.getAttribute('game-title') || this.getAttribute('gametitle') || ''; }
    attributeChangedCallback() { if (!this._init) return; this.glyphImg.src = this.getAttribute('glyph') || ''; this.glyphImg.style.display = this.getAttribute('glyph') ? 'block' : 'none'; this.cardGlyph.src = this.getAttribute('glyph') || ''; this.detail.textContent = this.title_() || ''; }
    connectedCallback() {
      if (this._init) return; this._init = true;
      Object.assign(this.style, { position: 'relative', display: 'block', width: '100%', height: '100%', overflow: 'hidden', background: '#04060C', userSelect: 'none', webkitUserSelect: 'none', containerType: 'size' });
      const U = n => `calc(${n} * 0.15cqw)`;
      this.bg = new Image(); this.bg.src = this.getAttribute('bg'); this.bg.onload = () => this.idle();
      this.cv = document.createElement('canvas'); Object.assign(this.cv.style, { position: 'absolute', inset: '0', width: '100%', height: '100%' });
      this.appendChild(this.cv); this.ro = setupCanvas(this, this.cv); new ResizeObserver(() => { if (!this.running) this.idle(); }).observe(this);
      // the action slot, centred on the orb baked into the screenshot's footer
      const slot = this.slot_ = document.createElement('div');
      Object.assign(slot.style, { position: 'absolute', left: '50%', top: '95.3%', width: '0', height: '0', transition: 'opacity .3s', touchAction: 'none', cursor: 'pointer' });
      slot.innerHTML = `
        <svg class="neck" viewBox="0 0 124 24" preserveAspectRatio="none" style="position:absolute;left:${U(-62)};bottom:0;width:${U(124)};height:${U(25)};opacity:0;transition:opacity .2s"><path d="M0 0 L124 0 C100 0 84 8 84 24 L40 24 C40 8 24 0 0 0Z" fill="${FILL}"/></svg>
        <div class="orb" style="position:absolute;left:${U(-22)};top:${U(-22)};width:${U(44)};height:${U(44)};border-radius:50%;display:grid;place-items:center;box-sizing:border-box;background:linear-gradient(135deg,${rgb(ACCENT, .62)},${rgb(lerpC(ACCENT, [0, 0, 0], .4), .5)});border:1.5px solid ${rgb(EDGE)};box-shadow:0 0 ${U(18)} ${rgb(EDGE, .45)};transition:background .2s,border-color .2s">
          <div style="width:${U(26)};height:${U(26)};border-radius:50%;background:#fff;display:grid;place-items:center;overflow:hidden"><img class="g" alt="A" style="width:${U(26)};height:${U(26)};object-fit:contain"></div>
        </div>
        <div class="card" style="position:absolute;left:0;bottom:${U(24)};transform:translateX(-50%) scale(.3);transform-origin:50% 100%;opacity:0;transition:opacity .16s,transform .18s;pointer-events:none;display:flex;align-items:center;gap:${U(12)};min-height:${U(68)};min-width:${U(220)};box-sizing:border-box;padding:${U(10)} ${U(20)};border-radius:${U(20)};background:${FILL};white-space:nowrap">
          <div style="position:relative;width:${U(34)};height:${U(34)};display:grid;place-items:center;flex:0 0 auto">
            <svg viewBox="0 0 100 100" style="position:absolute;inset:0;width:100%;height:100%;transform:rotate(-90deg)"><circle cx="50" cy="50" r="47" fill="none" stroke="rgba(255,255,255,.25)" stroke-width="6"/><circle class="arc" cx="50" cy="50" r="47" fill="none" stroke="#fff" stroke-width="6" stroke-linecap="round" pathLength="1" stroke-dasharray="0 1"/></svg>
            <img class="cg" alt="" style="width:${U(24)};height:${U(24)};object-fit:contain">
          </div>
          <div style="display:flex;flex-direction:column;gap:${U(1)};font-family:Sora,sans-serif;color:#fff">
            <span style="font-size:${U(13)};font-weight:500;line-height:1.15">Play</span>
            <span class="d" style="font-size:${U(10)};font-weight:300;line-height:1.15;color:rgba(255,255,255,.75);max-width:${U(280)};overflow:hidden;text-overflow:ellipsis"></span>
          </div>
        </div>`;
      this.appendChild(slot);
      this.neck = slot.querySelector('.neck'); this.orb = slot.querySelector('.orb'); this.card = slot.querySelector('.card'); this.arc = slot.querySelector('.arc');
      this.glyphImg = slot.querySelector('.g'); this.cardGlyph = slot.querySelector('.cg'); this.detail = slot.querySelector('.d');
      this.tip = document.createElement('div'); this.tip.textContent = 'Press and hold';
      Object.assign(this.tip.style, { position: 'absolute', left: '50%', top: 'calc(95.3% - 0.15cqw * 40)', transform: 'translate(-50%,-100%)', font: '400 12px Sora,sans-serif', letterSpacing: '.14em', textTransform: 'uppercase', color: 'rgba(255,255,255,.85)', whiteSpace: 'nowrap', pointerEvents: 'none', transition: 'opacity .3s', textShadow: '0 1px 8px rgba(0,0,0,.8)' });
      this.appendChild(this.tip);
      this.attributeChangedCallback();
      this.p = 0; this.holding = false;
      const down = e => { e.preventDefault(); if (!this.running) { this.holding = true; this.fill(); } };
      const up = () => { this.holding = false; };
      slot.addEventListener('pointerdown', down); window.addEventListener('pointerup', up); slot.addEventListener('pointerleave', up);
      this.vis = false; new IntersectionObserver(es => es.forEach(e => this.vis = e.isIntersecting), { threshold: .4 }).observe(this);
      this._kd = e => { if (this.vis && !e.repeat && (e.key === 'a' || e.key === 'A' || e.key === 'Enter') && !this.running) { this.holding = true; this.fill(); } };
      this._ku = e => { if (e.key === 'a' || e.key === 'A' || e.key === 'Enter') this.holding = false; };
      window.addEventListener('keydown', this._kd); window.addEventListener('keyup', this._ku);
    }
    disconnectedCallback() { window.removeEventListener('keydown', this._kd); window.removeEventListener('keyup', this._ku); cancelAnimationFrame(this.raf); cancelAnimationFrame(this.fr); }
    cardOut(on) {
      this.card.style.opacity = this.neck.style.opacity = on ? '1' : '0';
      this.card.style.transform = `translateX(-50%) scale(${on ? 1 : .3})`; this.card.style.transition = on ? 'opacity .2s,transform .26s' : 'opacity .16s,transform .18s';
      this.orb.style.background = on ? FILL : `linear-gradient(135deg,${rgb(ACCENT, .62)},${rgb(lerpC(ACCENT, [0, 0, 0], .4), .5)})`;
      this.orb.style.borderColor = on ? 'transparent' : rgb(EDGE);
      this.tip.style.opacity = on ? '0' : '1';
    }
    fill() {
      cancelAnimationFrame(this.fr); let last = performance.now(); this.cardOut(true);
      const step = now => {
        const dt = now - last; last = now;
        this.p = this.holding ? Math.min(1, this.p + dt / HOLD_MS) : Math.max(0, this.p - dt / 120);
        this.arc.setAttribute('stroke-dasharray', `${this.p} 1`);
        if (!this.holding) this.cardOut(false);
        if (this.p >= 1) { this.holding = false; this.p = 0; this.launch(); setTimeout(() => this.arc.setAttribute('stroke-dasharray', '0 1'), 300); return; }
        if (this.p > 0 || this.holding) this.fr = requestAnimationFrame(step);
      };
      this.fr = requestAnimationFrame(step);
    }
    ctx() { const c = this.cv.getContext('2d'); c.setTransform(this.cv._d, 0, 0, this.cv._d, 0, 0); return c; }
    cover(c, w, h, alpha = 1) {
      if (!this.bg.complete || !this.bg.naturalWidth) { c.fillStyle = '#04060C'; c.fillRect(0, 0, w, h); return; }
      const iw = this.bg.naturalWidth, ih = this.bg.naturalHeight, s = Math.max(w / iw, h / ih);
      c.save(); c.globalAlpha = alpha; c.drawImage(this.bg, (w - iw * s) / 2, (h - ih * s) / 2, iw * s, ih * s); c.restore();
    }
    // the cover's square in the background shot: crop="x y size" as fractions of its width, height and height
    crop() {
      const iw = this.bg.naturalWidth, ih = this.bg.naturalHeight, c = (this.getAttribute('crop') || '').split(/\s+/).map(Number);
      if (c.length === 3 && c.every(n => n >= 0)) return { x: c[0] * iw, y: c[1] * ih, s: c[2] * ih };
      const sx = .27 * iw, sy = .515 * ih, sh = .195 * ih; return { x: sx + (.474 * iw - sx - sh) / 2, y: sy, s: sh };
    }
    art(c, x, y, size, round) { // the Skyrim tile from the hover panel, square-cropped
      if (!this.bg.naturalWidth) return; const k = this.crop();
      c.save(); c.beginPath(); if (round) c.arc(x, y, size / 2, 0, Math.PI * 2); else { const r = size * .0343; c.roundRect(x - size / 2, y - size / 2, size, size, r); } c.clip();
      c.drawImage(this.bg, k.x, k.y, k.s, k.s, x - size / 2, y - size / 2, size, size); c.restore();
    }
    idle() { const c = this.ctx(); this.cover(c, this.cv._w, this.cv._h); }
    ui(show) { this.slot_.style.opacity = this.tip.style.opacity = show ? '1' : '0'; this.slot_.style.pointerEvents = show ? 'auto' : 'none'; if (!show) this.cardOut(false); if (!show) this.tip.style.opacity = '0'; }
    launch() {
      this.running = true; this.ui(false);
      const mode = this.getAttribute('mode') || 'disc', t0 = performance.now();
      const total = mode === 'disc' ? D.Total : LENS.END + LENS.FADE;
      this.dispatchEvent(new CustomEvent('launchstart', { bubbles: true }));
      const tick = now => {
        const ms = now - t0, c = this.ctx(), w = this.cv._w, h = this.cv._h;
        if (mode === 'disc') this.disc(c, w, h, Math.min(1, ms / D.Total)); else this.lens(c, w, h, ms);
        if (ms < total) this.raf = requestAnimationFrame(tick); else { this.running = false; this.idle(); this.ui(true); }
      };
      this.raf = requestAnimationFrame(tick);
    }
    disc(c, w, h, n) {
      const m = Math.min(w, h);
      const caseIn = LOS(ph(n, 0, at(500))), emerge = FOS(ph(n, at(500), FIF));
      const caseAlpha = caseIn * (1 - ph(n, at(1240), FIF));
      const discAlpha = ph(n, at(500), at(760)), sink = ph(n, FIF, SEF), spin = ph(n, SEF, DOF);
      const outAt = ms => at(D.OutStart + ms);
      const drop = ph(n, DOF, outAt(350)), blackout = ph(n, DOF, outAt(200));
      const slitOpen = ph(n, outAt(230), outAt(350)), slitClose = FOS(ph(n, outAt(350), outAt(650))), slitFade = ph(n, outAt(600), outAt(670));
      const leave = FOS(ph(n, ROF, 1)), sinkE = FOS(sink), close = FOS(ph(n, FIF, DOF));
      const drift = h * .5;
      this.cover(c, w, h);
      // vignette
      const cx = w / 2, cy = h / 2 + drift * sinkE, shut = close * (1 - leave);
      const rad = Math.max(.05, 1.3 - (1.3 - .42) * shut) * m;
      const inner = Math.min(1, Math.max(0, (close - .72) / .28)) * (1 - leave), dim = .96 * (1 - leave);
      const vg = c.createRadialGradient(cx, cy, 0, cx, cy, rad);
      vg.addColorStop(0, `rgba(0,0,0,${inner * dim})`); vg.addColorStop(.62, `rgba(0,0,0,${Math.max(inner, shut * .4) * dim})`); vg.addColorStop(1, `rgba(0,0,0,${shut * dim})`);
      c.fillStyle = vg; c.fillRect(0, 0, w, h);
      if (blackout > 0) { c.fillStyle = `rgba(5,3,2,${blackout * (1 - leave)})`; c.fillRect(0, 0, w, h); }
      // disc
      const ds = m * .72, grow = .62 + .38 * emerge, shrink = 1 - .12 * sinkE;
      const rot = (emerge * 160 + sinkE * 70 + spin * spin * 900 + drop * 600) * Math.PI / 180;
      const dx = cx + w * .1875 * Math.sin(Math.PI * emerge), dy = h / 2 + drift * sinkE + m * .648 * drop * drop;
      if (discAlpha > 0) {
        c.save(); c.globalAlpha = discAlpha; c.translate(dx, dy); c.rotate(rot); c.scale(grow * shrink, grow * shrink);
        this.art(c, 0, 0, ds, true);
        const r = ds / 2;
        if (c.createConicGradient) { const cg = c.createConicGradient(-Math.PI / 2, 0, 0); [[0, 0], [.18, .18], [.32, 0], [.62, .1], [.78, 0], [1, 0]].forEach(([s, a]) => cg.addColorStop(s, `rgba(255,255,255,${a})`)); c.fillStyle = cg; c.beginPath(); c.arc(0, 0, r, 0, Math.PI * 2); c.fill(); }
        c.strokeStyle = 'rgba(255,255,255,.3)'; c.lineWidth = 2; c.beginPath(); c.arc(0, 0, r - 1, 0, Math.PI * 2); c.stroke();
        c.strokeStyle = 'rgba(255,255,255,.16)'; c.lineWidth = r * .045; c.beginPath(); c.arc(0, 0, r * .21, 0, Math.PI * 2); c.stroke();
        c.fillStyle = '#050302'; c.beginPath(); c.arc(0, 0, r * .125, 0, Math.PI * 2); c.fill();
        c.restore();
      }
      // case
      if (caseAlpha > 0) {
        const cs = m * .648, s = (.9 + .1 * caseIn) * (1 - .2 * emerge);
        c.save(); c.globalAlpha = caseAlpha; c.translate(cx + w * -.177 * emerge, h / 2); c.scale(s, s);
        c.shadowColor = 'rgba(0,0,0,.6)'; c.shadowBlur = 60; c.shadowOffsetY = 20; c.fillStyle = '#15151C'; c.beginPath(); c.roundRect(-cs / 2, -cs / 2, cs, cs, cs * .0343); c.fill(); c.shadowColor = 'transparent';
        this.art(c, 0, 0, cs, false); c.restore();
      }
      // slit of light
      if (slitOpen > 0) {
        const a = slitOpen * (1 - slitFade), half = w * .573 / 2 * slitOpen * (1 - slitClose);
        if (a > 0 && half > 0) {
          const core = Math.max(2, h * .0037), y = h * .935, glow = core * 12;
          const lg = c.createLinearGradient(0, y - glow / 2, 0, y + glow / 2); lg.addColorStop(0, 'rgba(255,236,214,0)'); lg.addColorStop(.5, `rgba(255,236,214,${.55 * a})`); lg.addColorStop(1, 'rgba(255,236,214,0)');
          c.fillStyle = lg; c.fillRect(w / 2 - half, y - glow / 2, half * 2, glow);
          c.fillStyle = `rgba(255,255,255,${a})`; c.fillRect(w / 2 - half, y - core / 2, half * 2, core);
        }
      }
    }
    lens(c, w, h, ms) {
      const t = Math.min(ms, LENS.END), f = lensFrame(t, w, h), gc = ACCENT, has = !!this.bg.naturalWidth;
      this.cover(c, w, h);
      c.fillStyle = `rgba(4,6,12,${f.dim})`; c.fillRect(0, 0, w, h);
      if (f.artAlpha > 0 && has) {
        const k = this.crop(), S = Math.hypot(w, h);
        c.save(); c.globalAlpha = f.artAlpha; c.beginPath(); c.arc(f.rx, f.ry, f.artR, 0, Math.PI * 2); c.clip();
        c.save(); c.translate(f.rx, f.ry); c.rotate(f.artRot * Math.PI / 180); c.scale(f.artScale, f.artScale); c.drawImage(this.bg, k.x, k.y, k.s, k.s, -S / 2, -S / 2, S, S); c.restore();
        if (f.artDim > 0) { c.fillStyle = `rgba(4,6,12,${f.artDim})`; c.fillRect(0, 0, w, h); }
        c.restore();
      }
      if (f.discAlpha > 0) {
        const rd = f.discR; c.save(); c.globalAlpha = f.discAlpha;
        const rg = c.createRadialGradient(f.rx, f.ry, 0, f.rx, f.ry, rd);
        [[0, 'rgba(4,6,12,1)'], [.14, 'rgba(4,6,12,1)'], [.145, 'rgba(255,255,255,.25)'], [.16, 'rgba(255,255,255,.1)'], [.30, 'rgba(255,255,255,.1)'], [.305, 'rgba(255,255,255,0)'], [1, 'rgba(255,255,255,0)']].forEach(([o, col]) => rg.addColorStop(o, col));
        c.fillStyle = rg; c.beginPath(); c.arc(f.rx, f.ry, rd, 0, Math.PI * 2); c.fill();
        if (c.createConicGradient) { const cg = c.createConicGradient(-70 * Math.PI / 180, f.rx, f.ry); [[0, 0], [30 / 360, .22], [70 / 360, 0], [180 / 360, 0], [210 / 360, .16], [250 / 360, 0], [1, 0]].forEach(([o, a]) => cg.addColorStop(o, `rgba(255,255,255,${a})`)); c.fillStyle = cg; c.beginPath(); c.arc(f.rx, f.ry, rd, 0, Math.PI * 2); c.fill(); }
        c.restore();
      }
      if (f.glowAlpha > 0) {
        const inner = Math.min(.99, Math.max(0, f.artR / f.glowR)), rg = c.createRadialGradient(f.rx, f.ry, 0, f.rx, f.ry, f.glowR);
        rg.addColorStop(0, rgb(gc, 0)); rg.addColorStop(inner, rgb(gc, 0)); rg.addColorStop(Math.min(.995, inner + .005), rgb(gc)); rg.addColorStop(Math.min(.999, Math.max(inner + .01, .45)), rgb(gc, .33)); rg.addColorStop(1, rgb(gc, 0));
        c.save(); c.globalAlpha = f.glowAlpha; c.fillStyle = rg; c.fillRect(0, 0, w, h); c.restore();
      }
      if (f.waveAlpha > 0) { // the crossbar's waves, drawn faintly in the accent
        c.save(); c.globalAlpha = f.waveAlpha; c.strokeStyle = rgb(lerpC(gc, [255, 255, 255], .5), .35); c.lineWidth = 1;
        for (let i = 0; i < 9; i++) { c.beginPath(); for (let x = 0; x <= w; x += 8) { const y = h * .55 + Math.sin(x / w * Math.PI * 2 + i * .45 + ms / 900) * h * (.05 + i * .008) + (i - 4) * h * .006; x ? c.lineTo(x, y) : c.moveTo(x, y); } c.stroke(); }
        c.restore();
      }
      if (f.cardAlpha > 0 && has) { const k = this.crop(), s = Math.max(f.cardW, f.cardH); c.save(); c.globalAlpha = f.cardAlpha; c.beginPath(); c.roundRect(f.cardX, f.cardY, f.cardW, f.cardH, Math.min(f.cardR, f.cardW / 2, f.cardH / 2)); c.clip(); c.drawImage(this.bg, k.x, k.y, k.s, k.s, f.cardX + (f.cardW - s) / 2, f.cardY + (f.cardH - s) / 2, s, s); c.restore(); }
      drawMark(c, f.mark);
      if (f.titleAlpha > 0) { const fs = 24 * h / 462; c.save(); c.globalAlpha = f.titleAlpha; c.fillStyle = '#fff'; c.font = `200 ${fs}px Sora, sans-serif`; c.textAlign = 'center'; c.textBaseline = 'top'; c.fillText(this.title_() || '', w / 2, f.titleY); c.restore(); }
      if (ms > LENS.END) this.cover(c, w, h, Math.min(1, (ms - LENS.END) / LENS.FADE));
    }
  }
  // Hero: boot sequence plays once off-centre, then the mark rests while echo rings keep pulsing out of it.
  function heroFrame(c, t, w, h) {
    const wide = w > 1200, cx = wide ? w * .76 : w * .78, cy = wide ? h * .46 : h * .3;
    const size = wide ? Math.min(.34 * h, .24 * w) : Math.min(.22 * h, .3 * w), k = size / 132, ry = cy - 14 * k;
    const g = easeOut(progress(t, 0, 500)), cc = easeInOut(progress(t, 500, 850));
    const line = t < 500 ? 1.1 * w * g : mix(1.1 * w, 0, cc);
    const a = easeOut(progress(t, 700, 1000)), ring = easeOut(progress(t, 1150, 1650)), cres = easeOut(progress(t, 1450, 2100));
    c.clearRect(0, 0, w, h); c.fillStyle = '#04060C'; c.fillRect(0, 0, w, h);
    // resting horizon line
    const hz = easeOut(progress(t, 1900, 3200));
    if (hz > 0) { const lg = c.createLinearGradient(0, 0, w, 0); lg.addColorStop(0, 'rgba(255,255,255,0)'); lg.addColorStop(cx / w, `rgba(159,220,236,${.35 * hz})`); lg.addColorStop(1, 'rgba(255,255,255,0)'); c.fillStyle = lg; c.fillRect(0, ry - .5, w, 1); }
    const breathe = t > 2500 ? .5 + .5 * Math.sin((t - 2500) / 1400) : 1;
    const ga = a * (1 - .7 * ring) + (t > 2100 ? .25 + .15 * breathe : 0), gr = .22 * h;
    if (ga > 0) { const rg = c.createRadialGradient(cx, ry, 0, cx, ry, gr); rg.addColorStop(0, 'rgba(159,220,236,.22)'); rg.addColorStop(1, 'rgba(159,220,236,0)'); c.save(); c.globalAlpha = Math.min(1, ga); c.fillStyle = rg; c.fillRect(cx - gr, ry - gr, gr * 2, gr * 2); c.restore(); }
    const R = Math.hypot(Math.max(cx, w - cx), Math.max(cy, h - cy));
    const starts = [850, 1100, 1350]; if (t > 3000) { const p = 2400, n = Math.floor((t - 3000) / p); for (let i = Math.max(0, n - 2); i <= n; i++) starts.push(3000 + i * p); }
    starts.forEach(s => { const r = echoRing(t, s, s < 3000 ? 1800 : 5200, cx, ry, 4 * k, s < 3000 ? .62 * w : R, s < 3000 ? .42 : .28); if (!r) return; c.save(); c.globalAlpha = r.a * (wide ? 1 : .55); c.strokeStyle = '#fff'; c.lineWidth = r.sw; c.beginPath(); c.arc(r.x, r.y, r.r, 0, Math.PI * 2); c.stroke(); c.restore(); });
    const la = 1 - progress(t, 780, 850);
    if (la > 0 && line > 0) { const lg = c.createLinearGradient(cx - line / 2, 0, cx + line / 2, 0); lg.addColorStop(0, 'rgba(255,255,255,0)'); lg.addColorStop(.3, '#fff'); lg.addColorStop(.7, '#fff'); lg.addColorStop(1, 'rgba(255,255,255,0)'); c.save(); c.globalAlpha = la; c.fillStyle = lg; c.fillRect(cx - line / 2, ry - .75, line, 1.5); c.restore(); }
    const ms = 1 + .012 * Math.sin(progress(t, 2500, 2900) * Math.PI);
    if (wide) drawMark(c, { cx, cy, size, ms, lx: -24 * (1 - cres), rx: 24 * (1 - cres), crescentAlpha: easeOut(progress(t, 1450, 1950)), ringAlpha: ring, ringScale: mix(.85, 1, ring), dotY: -46 + 46 * easeInOut(progress(t, 1150, 1650)), dotScale: mix(.2, 1, a), dotAlpha: a });
  }
  class EchoHero extends HTMLElement {
    connectedCallback() {
      if (this._init) return; this._init = true;
      Object.assign(this.style, { position: 'absolute', inset: '0', display: 'block', background: '#04060C' });
      this.cv = document.createElement('canvas'); Object.assign(this.cv.style, { position: 'absolute', inset: '0', width: '100%', height: '100%', display: 'block' });
      this.appendChild(this.cv); this.ro = setupCanvas(this, this.cv);
      const delay = +(this.getAttribute('delay') || 0), t0 = performance.now() + delay;
      const tick = now => { const c = this.cv.getContext('2d'); c.setTransform(this.cv._d, 0, 0, this.cv._d, 0, 0); heroFrame(c, Math.max(0, now - t0), this.cv._w, this.cv._h); this.raf = requestAnimationFrame(tick); };
      this.raf = requestAnimationFrame(tick);
    }
    disconnectedCallback() { this.ro && this.ro.disconnect(); cancelAnimationFrame(this.raf); }
  }
  if (!customElements.get('echo-hero')) customElements.define('echo-hero', EchoHero);
  if (!customElements.get('echo-boot')) customElements.define('echo-boot', EchoBoot);
  if (!customElements.get('echo-launch')) customElements.define('echo-launch', EchoLaunch);
})();
