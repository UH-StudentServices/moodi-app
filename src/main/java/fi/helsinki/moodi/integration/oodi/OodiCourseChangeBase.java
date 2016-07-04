package fi.helsinki.moodi.integration.oodi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class OodiCourseChangeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    public List<Data> data;

    public static class Data implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("OPPIAINE")
        public String oppiaine;

        @JsonProperty("NIMI_SV")
        public String nimiSv;

        @JsonProperty("NIMI_EN")
        public String nimiEn;

        @JsonProperty("LAAJUUS")
        public String laajuus;

        @JsonProperty("OPETUSTYYPPI_ID")
        public Integer opetusTyyppiId;

        @JsonProperty("URL_FI")
        public String urlFi;

        @JsonProperty("URL_EN")
        public String urlEn;

        @JsonProperty("URL_SV")
        public String urlSv;

        @JsonProperty("KIELI")
        public List<String> kieli;

        @JsonProperty("NIMI_FI")
        public String nimiFi;

        @JsonProperty("KIRJALLISUUS")
        public List<OodiKirjallisuus> kirjallisuus;

        @JsonProperty("TUNNISTE")
        public String tunniste;

        @JsonProperty("KURSSIKUVAUS")
        public OodiKurssikuvaus oodiKurssikuvaus;
    }

    public static class OodiKirjallisuus implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("ISBN")
        public String isbn;

        @JsonProperty("TEKIJA")
        public String tekija;

        @JsonProperty("NIMI")
        public String nimi;
        
        @JsonProperty("PAINOS")
        public String painos;
        
        @JsonProperty("VUOSI")
        public Integer vuosi;
        
        @JsonProperty("JULKAISIJA")
        public String julkaisija;
        
        @JsonProperty("KIELI")
        public Integer kieli;
    }
    
    public static class OodiKurssikuvaus implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SISALTO_SV")
        public String sisaltoSv;

        @JsonProperty("KOHDERYHMA_SV")
        public String kohderyhmaSv;

        @JsonProperty("ARVIOINTI_SV")
        public String arviointiSv;

        @JsonProperty("ARVIOINTI_FI")
        public String arviointiFi;

        @JsonProperty("ARVIOINTI_EN")
        public String arviointiEn;

        @JsonProperty("SUORITUSTAVAT_FI")
        public String suoritusTavatFi;

        @JsonProperty("ED_OPINNOT_FI")
        public String edOpinnotFi;

        @JsonProperty("OPPIMATERIAALI_FI")
        public String oppimateriaaliFi;

        @JsonProperty("LISATIEDOT_EN")
        public String lisatiedotEn;

        @JsonProperty("AJOITUS_EN")
        public String ajoitusEn;

        @JsonProperty("LISATIEDOT_FI")
        public String lisatiedotFi;

        @JsonProperty("KOHDERYHMA_FI")
        public String kohderyhmaFi;

        @JsonProperty("TAVOITE_FI")
        public String tavoiteFi;

        @JsonProperty("ED_OPINNOT_SV")
        public String edOpinnotSv;

        @JsonProperty("ED_OPINNOT_EN")
        public String edOpinnotEn;

        @JsonProperty("OPPIMATERIAALI_EN")
        public String oppimateriaaliEn;

        @JsonProperty("SISALTO_FI")
        public String sisaltoFi;

        @JsonProperty("SISALTO_EN")
        public String sisaltoEn;

        @JsonProperty("AJOITUS_FI")
        public String ajoitusFi;

        @JsonProperty("AJOITUS_SV")
        public String ajoitusSv;

        @JsonProperty("KOHDERYHMA_EN")
        public String kohderyhmaEn;

        @JsonProperty("OPPIMATERIAALI_SV")
        public String oppimateriaaliSv;

        @JsonProperty("TAVOITE_EN")
        public String tavoiteEn;

        @JsonProperty("TAVOITE_SV")
        public String tavoiteSv;

        @JsonProperty("SUORITUSTAVAT_EN")
        public String suoritustavatEn;

        @JsonProperty("SUORITUSTAVAT_SV")
        public String suoritustavatSv;

        @JsonProperty("LISATIEDOT_SV")
        public String lisatiedotSv;

    }
}