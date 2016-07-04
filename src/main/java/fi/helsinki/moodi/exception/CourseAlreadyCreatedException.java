package fi.helsinki.moodi.exception;

public final class CourseAlreadyCreatedException extends MoodiException {

    public CourseAlreadyCreatedException(long realisationId) {
        super("Course already created with realisation id : " + realisationId);
    }
}
