module com.carnetentretien.carnetentretien {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires java.sql;

    opens com.carnetentretien.carnetentretien to javafx.fxml;
    exports com.carnetentretien.carnetentretien;

    opens com.carnetentretien.carnetentretien.controller to javafx.fxml;
}