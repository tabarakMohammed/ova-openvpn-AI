module iq.linux.ova {
    requires javafx.controls;
    requires javafx.fxml;

    //requires org.controlsfx.controls;

    opens iq.linux.ova to javafx.fxml;
    exports iq.linux.ova;
}