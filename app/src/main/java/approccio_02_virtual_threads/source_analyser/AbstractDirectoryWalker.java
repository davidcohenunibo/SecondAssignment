package approccio_02_virtual_threads.source_analyser;

import approccio_01_task.source_analyser.DirectoryWalkerParams;
import approccio_01_task.source_analyser.Walker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract class that provides a skeleton implementation of the directory walking functionality
 * for a given directory. Concrete subclasses can extend this class and implement the `walkRec`
 * method to perform the actual directory walking and file processing.
 * The `AbstractDirectoryWalker` class takes `DirectoryWalkerParams` object that represents the
 * parameters for configuring the directory walking behavior.
 */
public abstract class AbstractDirectoryWalker implements Walker {

    /**
     * The parameters for configuring the directory walking behavior.
     */
    protected final approccio_01_task.source_analyser.DirectoryWalkerParams params;
    protected final AtomicBoolean isRunning = new AtomicBoolean(false);

    public AbstractDirectoryWalker(DirectoryWalkerParams params) {
        this.params = params;
    }

    /**
     * Starts the directory walking process.
     *
     * @return true if the directory walking is successful, false otherwise
     */
    @Override
    public boolean walk() {
        this.isRunning.set(true);
        try {
            this.walkRec(this.params.getDirectory());
            this.stop();
            return true;
        } catch (Exception e) {
            System.err.println("An error occurred during the walk: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void stop() {
        this.isRunning.set(false);
        this.stopBehaviour();
    }

    /**
     * Performs the actual directory walking and file processing for the given directory.
     * This method is abstract and must be implemented by the concrete subclasses to provide
     * the specific behavior for the directory walking.
     *
     * @param directory the directory to be walked
     * @throws IOException if an I/O error occurs
     */
    protected abstract void walkRec(Path directory) throws IOException;

    protected abstract void stopBehaviour();
}