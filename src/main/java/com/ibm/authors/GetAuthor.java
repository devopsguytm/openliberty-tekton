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

@ApplicationScoped
@Path("/getauthor")
@OpenAPIDefinition(info = @Info(title = "Authors Service", version = "1.0", description = "Authors Service APIs", contact = @Contact(url = "https://github.com/nheidloff/cloud-native-starter", name = "Niklas Heidloff"), license = @License(name = "License", url = "https://github.com/nheidloff/cloud-native-starter/blob/master/LICENSE")))
public class GetAuthor {

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
			@QueryParam("name") String name) {
		
			Author author = new Author();
			author.name = "Vlad Sancira";
			author.twitter = "None";
			author.blog = "https://github.com/vladsancira/";

			Error notfound = new Error();
			notfound.error="Author not found.";
			notfound.code="404";

			Error tooshort = new Error();
			tooshort.error="Name too short. Minimum length is 3 characters.";
			tooshort.code="500";
				
			
			System.out.println("Request for name = "+name );

			if (name.length()<3) {
				System.out.println("Sending response :");
				System.out.println(this.createJson(tooshort));	
				return Response.ok(this.createJson(tooshort)).status(500).build();
			}

			if (author.name.toLowerCase().contains(name.toLowerCase())){
				System.out.println("Sending response :");
				System.out.println(this.createJson(author));
				return Response.ok(this.createJson(author)).status(200).build();
			} else 	{
				System.out.println("Sending response :");
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