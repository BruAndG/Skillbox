package searchengine.services;

public interface StartHandler {
    boolean isError();

    void setError(boolean error);

    boolean isOunPage();
}
