package my.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/swagger-initializer.js")
public class SwaggerInitializerResource {

    @GET
    @Produces("application/javascript")
    public Response get() {
        String js = " window.onload = function() {\n" + 
        		"          window.ui = SwaggerUIBundle({\n" + 
        		"            url: \"./openapi.json\",\n" + 
        		"            dom_id: '#swagger-ui',\n" + 
        		"            deepLinking: true,\n" + 
        		"            presets: [\n" + 
        		"              SwaggerUIBundle.presets.apis,\n" + 
        		"              SwaggerUIStandalonePreset\n" + 
        		"            ],\n" + 
        		"            plugins: [\n" + 
        		"              SwaggerUIBundle.plugins.DownloadUrl\n" + 
        		"            ],\n" + 
        		"            layout: \"StandaloneLayout\"\n" + 
        		"          });\n" + 
        		"        };";
        return Response.ok(js).build();
    }
}
