import okhttp3.*;

import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Download the first 4 chunks of a file.  Default chunk size is 1 MiB;
 */
public class CTDownload {
    public static void main(String[] args) {
        Options options = null;
        try {
            options = processArgs(args);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("\nBad arguments\n");
            showHhelp();
            System.exit(1);
        }
        new CTDownload(options);
        System.exit(0);
    }

    private static void showHhelp() {
        System.out.println("Usage: [OPTIONS}");
        System.out.println("-source url         This option is required.  It must be followed by a complete");
        System.out.println("                    URL, including the scheme, for the file to download.");
        System.out.println("-destionation file  This option is optional.   If used, it must be followed by a");
        System.out.println("                    path and file name for the location to download to.  The");
        System.out.println("                    path must exist. If not used the file is downloaded to the");
        System.out.println("                    current directory and named download.dat.");
        System.out.println("-parallel           When used, chunks are downloaded in parallel.  Otherwise,");
        System.out.println("                    chunks are downloaded sequentially.");
        System.out.println("-help               Describes options to user.");
    }

    private void error(Exception e) {
        System.out.println("An error occured and download did not complete:");
        e.printStackTrace();
        System.exit(1);
    }

    public CTDownload(Options options) {
        OkHttpClient client = new OkHttpClient();

        if (options.parallel) {
            try {
                parallelDownload(options);
            } catch (FileNotFoundException | InterruptedException e) {
                error(e);
            }
        } else {
            String destination = options.destination == null ? "download.dat" : options.destination;
            FileOutputStream fos = null;
            try {
                File file = new File(destination);
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                error(e);
            }
            try {
                long start = 0;
                long end = options.chunkSize - 1;
                synchronousDownload(client, "http://f39bf6aa.bwtest-aws.pravala.com/384MB.jar", start, end, fos);
                start += options.chunkSize;
                end += options.chunkSize;
                synchronousDownload(client, "http://f39bf6aa.bwtest-aws.pravala.com/384MB.jar", start, end, fos);
                start += options.chunkSize;
                end += options.chunkSize;
                synchronousDownload(client, "http://f39bf6aa.bwtest-aws.pravala.com/384MB.jar", start, end, fos);
                start += options.chunkSize;
                end += options.chunkSize;
                synchronousDownload(client, "http://f39bf6aa.bwtest-aws.pravala.com/384MB.jar", start, end, fos);
            } catch (IOException e) {
                error(e);
            }

            try {
                fos.close();
            } catch (IOException e) {
                error(e);
            }
        }
    }

    /**
     * Download a chunk of a fie and add it to the end of a FileOutputStream.
     * @param client download client.
     * @param url download url.
     * @param start start position for download.
     * @param end end position for download
     * @param fos stream to write to.
     * @throws IOException
     */
    void synchronousDownload(OkHttpClient client, String url, long start, long end, FileOutputStream fos) throws IOException {
        String range = "bytes=" + start + "-" + end;
        Request request = new Request.Builder().url(url)
                .addHeader("Range", range)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Failed to download chunk: " + response);
        }
        fos.write(response.body().bytes());
        fos.flush();
    }

    /**
     * Download the first four chunks of a file in parallel.
     * @param options options for downloadng.
     * @throws FileNotFoundException
     * @throws InterruptedException
     */
    void parallelDownload(Options options) throws FileNotFoundException, InterruptedException {
        OkHttpClient client = new OkHttpClient();
        String destination = options.destination == null ? "download.dat" : options.destination;
        RandomAccessFile out = new RandomAccessFile(destination,"rw");
        CountDownLatch countDownLatch = new CountDownLatch(4);

        // Fetch chunks in parallel.
        long start = 0;
        long end = options.chunkSize - 1;
        asynchronousDownload(client, options.source, start, end, out, countDownLatch);
        start += options.chunkSize;
        end += options.chunkSize;
        asynchronousDownload(client, options.source, start, end, out, countDownLatch);
        start += options.chunkSize;
        end += options.chunkSize;
        asynchronousDownload(client, options.source, start, end, out, countDownLatch);
        start += options.chunkSize;
        end += options.chunkSize;
        asynchronousDownload(client, options.source, start, end, out, countDownLatch);

        // Wait for the downloads to complete.
        if (!countDownLatch.await(20L, TimeUnit.SECONDS)) {
            System.out.println("Download timed out.  Aborting!");
            System.exit(1);
        }
        try {
            out.close();
        } catch (IOException e) {
            error(e);
        }
    }

    /**
     * Asynchronously download a chunk of a file.
     * @param client download client.
     * @param url download url.
     * @param start start position for download.
     * @param end end position for download
     * @param out file to write bytes to.
     * @param countDownLatch latch to synchronize download completion.
     */
    private void asynchronousDownload(OkHttpClient client, String url, long start, long end, RandomAccessFile out, CountDownLatch countDownLatch) {
        Thread thread = new Thread() {
            public void run() {
                String range = "bytes=" + start + "-" + end;
                Request request = new Request.Builder().url(url)
                        .addHeader("Range", range)
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        countDownLatch.countDown();
                    }

                    @Override public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody responseBody = response.body()) {
                            if (!response.isSuccessful()) {
                                throw new IOException("Failed to download chunk: " + response);
                            }

                            byte[] bytes = response.body().bytes();
                            synchronized (this) {
                                out.seek(start);
                                out.write(bytes);
                            }
                        }
                        countDownLatch.countDown();
                    }
                });
            }
        };

        thread.start();
    }

    /**
     * Convert command line arguments to download options.
     *
     * @param args the command line arguments
     * @return download options.
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    static Options processArgs(String[] args) throws IllegalArgumentException, IllegalStateException {
        OptionsBuilder builder = new OptionsBuilder();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-source":
                    if (i + 1 < args.length) {
                        i++;
                        builder.source(args[i]);
                    } else {
                        throw new IllegalArgumentException("Missing value for source");
                    }
                    break;
                case "-destination":
                    if (i + 1 < args.length) {
                        i++;
                        builder.destination(args[i]);
                    } else {
                        throw new IllegalArgumentException("Missing value for destination");
                    }
                    break;
                case "-parallel":
                    builder.parallel();
                    break;
                case "-help":
                    showHhelp();
                    System.exit(0);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument:" + args[i]);
            }

        }
        return builder.build();
    }

    /**
     * Options for downloading.
     */
    protected static class Options {
        String source;
        String destination;
        boolean parallel;
        long chunkSize = 1048576; // Default chunk size in bytes: 1 MiB.

        /**
         * Construct options for downloading.
         * @param source the source for the file download.  Cannot be null.
         * @param destination the destination for the file download.
         * @param parallel when true, download in parallel.
         * @throws IllegalStateException thrown when source is null.
         */
        Options(String source, String destination, boolean parallel) throws IllegalStateException {
            if (source == null) {
                throw new IllegalStateException("Source cannot be null");
            }
            this.source = source;
            this.destination = destination;
            this.parallel = parallel;
        }
    }

    /**
     * A utility class used to build Options for downloadng.  Takes optional values in any order.
     */
    private static class OptionsBuilder {
        String source;
        String destination;
        boolean parallel;

        OptionsBuilder source(String source) {
            this.source = source;
            return this;
        }

        OptionsBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        OptionsBuilder parallel() {
            parallel = true;
            return this;
        }

        Options build() {
            return new Options(source, destination, parallel);
        }
    }
}
