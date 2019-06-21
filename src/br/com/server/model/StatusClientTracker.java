package br.com.server.model;

/**
 *
 * @author Guilherme Alves
 */
public class StatusClientTracker {

    public static final int PROCESS = 1;
    public static final int TIMEOUT = 2;

    private StatusClientTracker() {
        throw new IllegalArgumentException("No StatusClientTracker!");
    }
}
