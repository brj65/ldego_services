package tech.bletchleypark.exceptions;

public class InvalidSessionException extends Throwable {

    public InvalidSessionException(){
    }
    public InvalidSessionException(Throwable cause){
        super(cause);
    }
    public InvalidSessionException(String string) {
        super(string);
    }
}
