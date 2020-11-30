package fi.helsinki.moodi.test;

import java.util.Map;

public class SisuServerRequest {
    public String query;
    public Map<String, Object> variables;

    public SisuServerRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }
}
