package pt.acv.adega.processos.vindima;

import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Trabalhador;
import pt.acv.adega.planeamento.RegistoVindima;

import java.util.ArrayList;
import java.util.List;

/**
 * Dados da vindima de uma linha do planeamento, submetidos na folha da Fase 2.
 * responsavel/adegaEntrega ligam por id (DomainClassConverter). As vindimas sao
 * as 5 linhas da sub-tabela (as vazias sao ignoradas ao guardar).
 */
public class VindimaLinhaForm {

    private Trabalhador responsavel;
    private Adega adegaEntrega;
    private String vasilame;
    private String meios;
    private String metodos;
    private String transporte;
    private String observacoes;
    private List<RegistoVindima> vindimas = new ArrayList<>();

    public Trabalhador getResponsavel() { return responsavel; }
    public void setResponsavel(Trabalhador responsavel) { this.responsavel = responsavel; }

    public Adega getAdegaEntrega() { return adegaEntrega; }
    public void setAdegaEntrega(Adega adegaEntrega) { this.adegaEntrega = adegaEntrega; }

    public String getVasilame() { return vasilame; }
    public void setVasilame(String vasilame) { this.vasilame = vasilame; }

    public String getMeios() { return meios; }
    public void setMeios(String meios) { this.meios = meios; }

    public String getMetodos() { return metodos; }
    public void setMetodos(String metodos) { this.metodos = metodos; }

    public String getTransporte() { return transporte; }
    public void setTransporte(String transporte) { this.transporte = transporte; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public List<RegistoVindima> getVindimas() { return vindimas; }
    public void setVindimas(List<RegistoVindima> vindimas) { this.vindimas = vindimas; }
}
