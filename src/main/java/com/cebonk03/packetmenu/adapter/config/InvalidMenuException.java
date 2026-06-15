package com.cebonk03.packetmenu.adapter.config;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a menu configuration file is structurally invalid.
 *
 * <p>Carries the file name, optional line number, the configuration node path
 * where the error occurred, and a descriptive message.  This is a checked
 * exception; callers that cannot recover should wrap it in a
 * {@link RuntimeException}.
 */
public class InvalidMenuException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String file;
    private final @Nullable Integer lineNumber;
    private final String nodePath;

    /**
     * Creates a new {@code InvalidMenuException}.
     *
     * @param file       the file name where the error occurred
     * @param lineNumber the line number, or {@code null} if unknown
     * @param nodePath   the configuration node path (e.g. {@code items.sword.material})
     * @param message    a human-readable description of the problem
     */
    public InvalidMenuException(
            final String file,
            final @Nullable Integer lineNumber,
            final String nodePath,
            final String message
    ) {
        super(message);
        this.file = file;
        this.lineNumber = lineNumber;
        this.nodePath = nodePath;
    }

    /**
     * Creates a new {@code InvalidMenuException} without a line number.
     *
     * @param file     the file name where the error occurred
     * @param nodePath the configuration node path
     * @param message  a human-readable description of the problem
     */
    public InvalidMenuException(
            final String file,
            final String nodePath,
            final String message
    ) {
        this(file, null, nodePath, message);
    }

    /**
     * Creates a new {@code InvalidMenuException} for structural issues (e.g.
     * inheritance cycles, missing parents) where no specific file location is
     * known.
     *
     * @param message a human-readable description of the problem
     */
    public InvalidMenuException(final String message) {
        this("<unknown>", "<unknown>", message);
    }

    /**
     * Creates a new {@code InvalidMenuException} with a cause.
     *
     * @param message a human-readable description of the problem
     * @param cause   the root cause
     */
    public InvalidMenuException(final String message, final Throwable cause) {
        this("<unknown>", "<unknown>", message);
        initCause(cause);
    }

    /**
     * Returns the file name where the error occurred.
     *
     * @return the file name
     */
    public String getFile() {
        return file;
    }

    /**
     * Returns the line number where the error occurred, if known.
     *
     * @return the line number, or {@code null}
     */
    public @Nullable Integer getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the configuration node path where the error occurred.
     *
     * @return the node path
     */
    public String getNodePath() {
        return nodePath;
    }

    @Override
    public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append("In ").append(file);
        if (lineNumber != null) {
            sb.append(':').append(lineNumber);
        }
        sb.append(" at '").append(nodePath).append("': ");
        sb.append(super.getMessage());
        return sb.toString();
    }
}
