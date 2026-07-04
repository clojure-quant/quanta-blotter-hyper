;(function () {
  let audioCtx = null

  function beepOnce() {
    try {
      const Ctx = window.AudioContext || window.webkitAudioContext
      if (!Ctx) return
      if (!audioCtx) audioCtx = new Ctx()
      if (audioCtx.state === "suspended") {
        void audioCtx.resume()
      }

      const osc = audioCtx.createOscillator()
      const gain = audioCtx.createGain()
      const t0 = audioCtx.currentTime

      osc.type = "sine"
      osc.frequency.setValueAtTime(880, t0)
      gain.gain.setValueAtTime(0.0001, t0)
      gain.gain.exponentialRampToValueAtTime(0.2, t0 + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.35)

      osc.connect(gain)
      gain.connect(audioCtx.destination)
      osc.start(t0)
      osc.stop(t0 + 0.35)
    } catch {
      /* audio unavailable */
    }
  }

  window.quantaBeep = beepOnce
})()
