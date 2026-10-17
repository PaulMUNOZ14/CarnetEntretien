module com.carnetentretien.carnetentretien {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;

    opens com.carnetentretien.carnetentretien to javafx.fxml;
    exports com.carnetentretien.carnetentretien;
}