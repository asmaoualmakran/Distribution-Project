package data.readers;

import data.models.Comment;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.BasicConfigurator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Sam van den Vonder
 * Usage:
 *   Reading all comments from a file into a list of Comment objects:
 *     List<Comment> comments = RedditCommentLoader.readData(<filepath>);
 *         <filepath>: example path to data file: "./src/be/vub/parallelism/data/files/RC_2010-01.json" (i.e. from the root of the project directory)
 *   Reading only comments from a file into a list of Comment objects where some condition holds:
 *     List<Comment> comments = RedditCommentLoader.readData(<filepath>, <predicate>);
 *         <filepath>: same as above
 *         <predicate>: a lambda with type signature "Comment -> boolean". For example: comment -> comment.subreddit.equals("BMW")
 */
public class RedditCommentLoader {

    /**
     * Read a file of Reddit comments and return an in-memory list of objects
     *
     * @param dataPath Path to the data, e.g. "be.vub.parallelism.data/data.json"
     * @return A list of comments
     */
    static public List<Comment> readData(String dataPath) throws IOException {
        InputStream dataStream = getInputStream(dataPath);
        return readJsonStream(dataStream);
    }


    /**
     * Read a file of Reddit comments and return in-memory objects mapped to their ID
     *
     * @param dataPath Path to the data, e.g. "be.vub.parallelism.data/data.json"
     * @param shouldInclude A lambda that determines whether a particular comment should be included in the result set
     * @return A list of comments
     */
    static public List<Comment> readData(String dataPath, Function<Comment, Boolean> shouldInclude) throws IOException {
        InputStream dataStream = getInputStream(dataPath);
        return readJsonStream(dataStream, shouldInclude);
    }



    /**
     * Read multiple files of Reddit comments and return an in-memory list of objects
     *
     * @param dataPaths Array of paths to the data, e.g. ["be.vub.parallelism.data/data.json"]
     * @return A list of comments
     */
    static public List<Comment> readData(String[] dataPaths) throws IOException {
        return readData(dataPaths, any -> true);
    }

    /**
     * Read multiple files of Reddit comments and return an in-memory list of objects
     *
     * @param dataPaths Array of paths to the data, e.g. ["be.vub.parallelism.data/data.json"]
     * @param shouldInclude A lambda that determines whether a particular comment should be included in the result set
     * @return A list of comments
     */
    static public List<Comment> readData(String[] dataPaths, Function<Comment, Boolean> shouldInclude) throws IOException {
        if (dataPaths.length == 0)
            return new ArrayList<>();
        else if (dataPaths.length == 1)
            return readData(dataPaths[0], shouldInclude);
        else {
            InputStream firstFile = getInputStream(dataPaths[0]);
            InputStream secondFile = getInputStream(dataPaths[1]);
            SequenceInputStream sequencedInputStreams = new SequenceInputStream(firstFile, secondFile);

            for (int i=2; i<dataPaths.length; i++) {
                InputStream ithInputStream = getInputStream(dataPaths[i]);
                sequencedInputStreams = new SequenceInputStream(ithInputStream, sequencedInputStreams);
            }

            return readJsonStream(sequencedInputStreams, shouldInclude);
        }
    }



    /**
     * @param inputStream A stream of data
     * @return A list of comments
     */
    static private List<Comment> readJsonStream(InputStream inputStream) throws IOException {
        return readJsonStream(inputStream, any -> true);
    }


    /**
     * @param inputStream A stream of data
     * @param shouldInclude A lambda that determines whether a particular comment should be included in the result set
     * @return A list of comments
     */
    static private List<Comment> readJsonStream(InputStream inputStream, Function<Comment, Boolean> shouldInclude) throws IOException {
        Gson gson = createBuilder();
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        JsonStreamParser parser = new JsonStreamParser(reader);

        List<Comment> comments = new ArrayList<Comment>();
        while (parser.hasNext()) {
            JsonElement e = parser.next();
            if (e.isJsonObject()) {
                Comment comment = gson.fromJson(e, Comment.class);
                if (shouldInclude.apply(comment))
                    comments.add(comment);
            }
        }
        reader.close();
        return comments;
    }


    /**
     * Create a builder for JSON data
     *
     * @return a GSON builder for JSON data
     */
    static private Gson createBuilder() {
        GsonBuilder builder = new GsonBuilder();
        return builder.create();
    }

    /**
     * Given a path to a file, turn it into a stream of data
     *
     * @param dataPath Path to a file
     * @return A stream of data
     */
    static private InputStream getInputStream(String dataPath) throws FileNotFoundException {
        return new FileInputStream(dataPath);
    }
}