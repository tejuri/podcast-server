package lan.dk.podcastserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import io.vavr.control.Option;
import lan.dk.podcastserver.manager.worker.updater.DailymotionUpdaterTest;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
public class JsonServiceTest {

    private UrlService urlService;
    private JsonService jsonService;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void beforeEach() {
        urlService = mock(UrlService.class);
        jsonService = new JsonService(urlService, mapper);
    }

    @Test
    public void should_read_json_from_files() throws URISyntaxException, IOException {
        /* Given */
        when(urlService.asReader(anyString())).thenReturn(IOUtils.fileAsReader("/remote/downloader/dailymotion/user.karimdebbache.json"));

        /* When */
        Option<DocumentContext> aFakeUrl = jsonService.parseUrl("http://foo.com/");

        /* Then */
        assertThat(aFakeUrl.toJavaOptional()).isPresent();
    }

    @Test
    public void should_return_empty_if_error_during_parsing() throws IOException {
        /* Given */
        doThrow(IOException.class).when(urlService).asReader(anyString());

        /* When */
        Option<DocumentContext> aFakeUrl = jsonService.parseUrl("http://foo.com/");

        /* Then */
        assertThat(aFakeUrl.toJavaOptional()).isEmpty();
    }
    
    @Test
    public void should_parse_from_string() {
        /* Given */
        String object = "{ \"foo\" : \"bar\"}";
        /* When */
        DocumentContext parse = jsonService.parse(object);

        /* Then */
        assertThat(parse.read("foo", String.class)).isEqualTo("bar");
    }

    @Test(expected = InvalidJsonException.class)
    public void should_return_empty_if_error_during_parsing_string() {
        /* Given */
        String object = "}{{{";

        /* When */
        jsonService.parse(object);

        /* Then @See annotation */
    }
}
