module org.shiyi.zzuli_drcom {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires java.prefs;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens org.shiyi.zzuli_drcom to javafx.fxml;
    exports org.shiyi.zzuli_drcom;
}