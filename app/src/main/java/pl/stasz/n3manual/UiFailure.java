package pl.stasz.n3manual;

/** Resource keys stay separate from protocol bytes; also usable by JVM tests. */
public final class UiFailure extends RuntimeException {
    public final String key;
    public final Object[] arguments;
    public UiFailure(String key,Object... arguments) {
        super(key);this.key=key;this.arguments=arguments.clone();
    }
}
