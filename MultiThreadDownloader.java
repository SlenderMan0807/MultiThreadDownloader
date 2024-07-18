import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MultiThreadDownloader {

    private static final int THREAD_COUNT = 4;  

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java MultiThreadDownloader <URL> <output file>");
            return;
        }
        
        String fileURL = args[0];
        String outputFile = args[1];
        
        try {
            downloadFile(fileURL, outputFile, THREAD_COUNT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadFile(String fileURL, String outputFile, int threadCount) throws Exception {
        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileSize = connection.getContentLength();
        connection.disconnect();

        RandomAccessFile output = new RandomAccessFile(outputFile, "rw");
        output.setLength(fileSize);
        output.close();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int partSize = fileSize / threadCount;
        
        for (int i = 0; i < threadCount; i++) {
            int startByte = i * partSize;
            int endByte = (i == threadCount - 1) ? fileSize : startByte + partSize - 1;
            executor.execute(new DownloadTask(fileURL, outputFile, startByte, endByte));
        }
        
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        
        System.out.println("Download completed.");
    }

    static class DownloadTask implements Runnable {
        private String fileURL;
        private String outputFile;
        private int startByte;
        private int endByte;

        public DownloadTask(String fileURL, String outputFile, int startByte, int endByte) {
            this.fileURL = fileURL;
            this.outputFile = outputFile;
            this.startByte = startByte;
            this.endByte = endByte;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
                String byteRange = startByte + "-" + endByte;
                connection.setRequestProperty("Range", "bytes=" + byteRange);
                InputStream inputStream = connection.getInputStream();

                RandomAccessFile output = new RandomAccessFile(outputFile, "rw");
                output.seek(startByte);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.close();
                inputStream.close();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
