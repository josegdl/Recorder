package intelite.models;

public class Canal {

    private String nombre;
    private String alias;
    private String origen;
    private String destino;
    private Integer activo;
    private String[] horario_g;
    private String[] horario_e;

    public Canal() {
        super();
    }

    public Canal(String nombre) {
        super();
        this.nombre = nombre;
        this.activo = 1;
    }

    public Canal(String nombre, String alias, String origen, String destino, Integer activo, String[] horario_g, String[] horario_e) {
        super();
        this.nombre = nombre;
        this.alias = alias;
        this.origen = origen;
        this.destino = destino;
        this.activo = activo;
        this.horario_g = horario_g;
        this.horario_e = horario_e;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }

    public String[] getHorario_g() {
        return horario_g;
    }

    public void setHorario_g(String[] horario_g) {
        this.horario_g = horario_g;
    }

    public String[] getHorario_e() {
        return horario_e;
    }

    public void setHorario_e(String[] horario_e) {
        this.horario_e = horario_e;
    }

    @Override
    public String toString() {
        return "Canal{" + "nombre=" + nombre + ", alias=" + alias + ", origen=" + origen + ", destino=" + destino + ", activo=" + activo + '}';
    }

}
