package com.ibm.authors;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import javax.json.Json;
import java.util.Base64;
import java.nio.charset.StandardCharsets;


@ApplicationScoped
@Path("/getauthor")
@OpenAPIDefinition(info = @Info(title = "Authors Service", version = "1.0", description = "Authors Service APIs", contact = @Contact(url = "https://github.com/nheidloff/cloud-native-starter", name = "Niklas Heidloff"), license = @License(name = "License", url = "https://github.com/nheidloff/cloud-native-starter/blob/master/LICENSE")))
public class GetAuthor {

	private static final String validapikey="anaaremeresipere";

	@GET
	@APIResponses(value = {
		@APIResponse(
	      responseCode = "404",
		  description = "Author Not Found",
		  content = @Content(
	        mediaType = "application/json",
	        schema = @Schema(implementation = Error.class)
	      )
	    ),
	    @APIResponse(
	      responseCode = "200",
	      description = "Author with requested name",
	      content = @Content(
	        mediaType = "application/json",
	        schema = @Schema(implementation = Author.class)
	      )
	    ),
	    @APIResponse(
	      responseCode = "500",
		  description = "Internal service error",
		  content = @Content(
	        mediaType = "application/json",
	        schema = @Schema(implementation = Error.class)
	      )  	      
	    )
	})
	@Operation(
		    summary = "Get specific author",
		    description = "Get specific author"
	)
	public Response getAuthor(@Parameter(
            description = "The unique name of the author",
            required = true,
            example = "Vlad Sancira",
            schema = @Schema(type = SchemaType.STRING))
			@QueryParam("name") String name,
			@Parameter(
            description = "The API KEY",
            required = true,
            example = "YW5hYXJlbWVyZXNpcGVyZQ==",
            schema = @Schema(type = SchemaType.STRING))
			@QueryParam("apikey") String apikey
			){

			Author author  = new Author("Vlad Sancira","none","https://github.com/vladsancira/");
			Error notfound = new Error("Author not found.","404");
			Error tooshort = new Error("Name too short. Minimum length is 3 characters.","500");
			Error noapikey = new Error("Invalid API KEY.","500");		
		
			String decodeAPIKEY;

			try {
				decodeAPIKEY = new String(Base64.getDecoder().decode(apikey.getBytes()), StandardCharsets.UTF_8);
			} catch (Exception e){
				decodeAPIKEY = "invalid";
			}	

			if ( ! validapikey.equalsIgnoreCase(decodeAPIKEY)) {
				System.out.println("Sending 500 response :");
				System.out.println(this.createJson(noapikey));	
				return Response.ok(this.createJson(noapikey)).status(500).build();
			}

			System.out.println("Request for name = "+name );

			if (name.length()<3) {
				System.out.println("Sending 500 response :");
				System.out.println(this.createJson(tooshort));	
				return Response.ok(this.createJson(tooshort)).status(500).build();
			}						

			if (author.name.toLowerCase().contains(name.toLowerCase())){
				System.out.println("Sending 200 response :");				
				System.out.println(this.createJson(author));
				return Response.ok(this.createJson(author)).status(200).build();
			} else 	{
				System.out.println("Sending 404 response :");
				System.out.println(this.createJson(notfound));				
				return Response.ok(this.createJson(notfound)).status(404).build();
			}				
			
	}

	public JsonObject createJson(Author author) {
		return Json.createObjectBuilder().add("name", author.name).add("twitter", author.twitter)
				.add("blog", author.blog).build();
	}

	public JsonObject createJson(Error error) {
		return Json.createObjectBuilder().add("description", error.error).add("code", error.code)
				.build();
	}
	
}