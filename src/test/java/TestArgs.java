import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class TestArgs {

    @Test
    void testBadCommandline() {
        String[] emptyArgs = {};
        assertThrows(IllegalStateException.class, () -> {
            CTDownload.processArgs(emptyArgs);
        });

        String[] noSourceArgs = {"-source"};
        assertThrows(IllegalArgumentException.class, () -> {
            CTDownload.processArgs(noSourceArgs);
        });

        String[] noDestinationArgs = {"-destination"};
        assertThrows(IllegalArgumentException.class, () -> {
            CTDownload.processArgs(noDestinationArgs);
        });

        String[] unknownArgs = {"-fooBar"};
        assertThrows(IllegalArgumentException.class, () -> {
            CTDownload.processArgs(unknownArgs);
        });
    }

    @Test
    void testOptions() {
        assertThrows(IllegalStateException.class, () -> {
            new CTDownload.Options(null, null, false);
        });
    }

    @Test
    void testGoodCommandLine() {
        String[] parallelArgs = {"-parallel", "-source", "http://test.com"};
        CTDownload.Options parallelOptions = CTDownload.processArgs(parallelArgs);
        assertTrue(parallelOptions.parallel, "Expected parallel to be true");

        String[] destinationArgs = {"-parallel", "-source", "http://test.com", "-destination", "download.dat"};
        CTDownload.Options destinationOptions = CTDownload.processArgs(destinationArgs);
        assertTrue(destinationOptions.destination != null && destinationOptions.destination.equals("download.dat"), "Expected destination to be populated");
    }
}
