import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.json.JsonObject;
import static org.junit.jupiter.api.Assertions.*;
import com.ibm.authors.*;


class GetAuthorTest {

    @Test
    @DisplayName("should return the json format")
    void testCreateJson() {

        Author author = new Author();
        author.name = "Niklas Heidloff";
        author.twitter = "https://twitter.com/nheidloff";
        author.blog = "http://heidloff.net";

        GetAuthor getAuthor = new GetAuthor();

        JsonObject json_author;
        json_author = getAuthor.createJson(author);



        assertEquals(json_author.getString("name"),author.name
                );
    }

}