package ie.gmit.sw;


/**
 * Jrec is a record or a read-only bean class with a constructor that
 * matches the set of parameters in the record signature and a suite of
 * accessor methods. The default implementation of the methods equals() and
 * hashCode() aggregate all the attributes of the record. This record is in charge
 * of managing our Jar Records.
 */

public record Jrec(String className, String packageName, Boolean isInterface, int sloc) {
}