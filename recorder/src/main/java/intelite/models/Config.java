package intelite.models;

import org.apache.commons.lang3.math.Fraction;

public class Config {

    private String fto; // Formato
    private Long dur; // Duración bloque
    private Integer res_w; // Resolución ancho (width)
    private Integer res_h; // Resolución alto (high)
    private Long vbr; // Bitrate video
    private Fraction fps; // Fotogramas por segundo
    private Integer asr; // Samplerate audio
    private Long abr; // Bitrate audio

    public Config() {
        super();
    }

    public Config(String fto, Long dur, Integer res_w, Integer res_h, Long vbr, Fraction fps, Integer asr, Long abr) {
        this.fto = fto;
        this.dur = dur;
        this.res_w = res_w;
        this.res_h = res_h;
        this.vbr = vbr;
        this.fps = fps;
        this.asr = asr;
        this.abr = abr;
    }

    public String getFto() {
        return fto;
    }

    public void setFto(String fto) {
        this.fto = fto;
    }

    public Long getDur() {
        return dur;
    }

    public void setDur(Long dur) {
        this.dur = dur;
    }

    public Integer getRes_w() {
        return res_w;
    }

    public void setRes_w(Integer res_w) {
        this.res_w = res_w;
    }

    public Integer getRes_h() {
        return res_h;
    }

    public void setRes_h(Integer res_h) {
        this.res_h = res_h;
    }

    public Long getVbr() {
        return vbr;
    }

    public void setVbr(Long vbr) {
        this.vbr = vbr;
    }

    public Fraction getFps() {
        return fps;
    }

    public void setFps(Fraction fps) {
        this.fps = fps;
    }

    public Integer getAsr() {
        return asr;
    }

    public void setAsr(Integer asr) {
        this.asr = asr;
    }

    public Long getAbr() {
        return abr;
    }

    public void setAbr(Long abr) {
        this.abr = abr;
    }

}
