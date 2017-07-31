package org.silentsoft.actlist.plugin.messagebox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.sun.javafx.stage.StageHelper;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;

@SuppressWarnings("restriction")
public final class MessageBox {
	
	private static List<Image> getActlistIcons() {
		return new Function<int[], List<Image>>() {
			@Override
			public List<Image> apply(int[] values) {
				ArrayList<Image> images = new ArrayList<Image>();
				for (int size : values) {
					images.add(new Image(String.join("", "/images/icon/actlist_", String.valueOf(size), ".png")));
				}
				return images;
			}
		}.apply(new int[]{24, 32, 48, 64, 128, 256});
	}
	
	public static void showAbout(String message) {
		showAbout(null, null, message);
	}
	
	public static void showAbout(Object owner, String message) {
		showAbout(owner, null, message);
	}
	
	public static void showAbout(String masthead, String message) {
		showAbout(null, masthead, message);
	}
	
	public static void showAbout(Object owner, String masthead, String message) {
		String title = "About";
		
		Alert alert = new Alert(AlertType.INFORMATION);
		((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(getActlistIcons());
		alert.setTitle(title);
		alert.setHeaderText(masthead);
		alert.setContentText(message);
		
		if (owner == null) {
			ObservableList<Stage> stages = StageHelper.getStages();
			if (stages.isEmpty() == false) {
				alert.initOwner(stages.get(0));
			}
		} else {
			alert.initOwner((Window) owner);
		}
		
		alert.showAndWait();
	}
	
	public static void showInformation(Object owner, String masthead, String message) {
		String title = "Information";
		
		Alert alert = new Alert(AlertType.INFORMATION);
		((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(getActlistIcons());
		alert.setTitle(title);
		alert.setHeaderText(masthead);
		alert.setContentText(message);
		
		if (owner == null) {
			ObservableList<Stage> stages = StageHelper.getStages();
			if (stages.isEmpty() == false) {
				alert.initOwner(stages.get(0));
			}
		} else {
			alert.initOwner((Window) owner);
		}
		
		alert.showAndWait();
	}
	
	public static Optional<ButtonType> showConfirm(String message) {
		return showConfirm(null, null, message);
	}
	
	public static Optional<ButtonType> showConfirm(Object owner, String message) {
		return showConfirm(owner, null, message);
	}
	
	public static Optional<ButtonType> showConfirm(String masthead, String message) {
		return showConfirm(null, masthead, message);
	}
	
	public static Optional<ButtonType> showConfirm(Object owner, String masthead, String message) {
		String title = "Confirm";
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
		((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(getActlistIcons());
		alert.setTitle(title);
		alert.setHeaderText(masthead);
		alert.setContentText(message);
		
		if (owner == null) {
			ObservableList<Stage> stages = StageHelper.getStages();
			if (stages.isEmpty() == false) {
				alert.initOwner(stages.get(0));
			}
		} else {
			alert.initOwner((Window) owner);
		}
		
		return alert.showAndWait();
	}
	
	public static void showError(String message) {
		showError(null, null, message);
	}
	
	public static void showError(Object owner, String message) {
		showError(owner, null, message);
	}
	
	public static void showError(String masthead, String message) {
		showError(null, masthead, message);
	}
	
	public static void showError(Object owner, String masthead, String message) {
		String title = "Error";
		
		Alert alert = new Alert(AlertType.ERROR);
		((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(getActlistIcons());
		alert.setTitle(title);
		alert.setHeaderText(masthead);
		alert.setContentText(message);
		
		if (owner == null) {
			ObservableList<Stage> stages = StageHelper.getStages();
			if (stages.isEmpty() == false) {
				alert.initOwner(stages.get(0));
			}
		} else {
			alert.initOwner((Window) owner);
		}
		
		alert.showAndWait();
	}
	
	public static void showException(Throwable exception) {
		showException(null, null, null, exception);
	}
	
	public static void showException(Object owner, Throwable exception) {
		showException(owner, null, null, exception);
	}
	
	public static void showException(String message, Throwable exception) {
		showException(null, null, message, exception);
	}
	
	public static void showException(Object owner, String message, Throwable exception) {
		showException(owner, null, message, exception);
	}
	
	public static void showException(String masthead, String message, Throwable exception) {
		showException(null, masthead, message, exception);
	}
	
	public static void showException(Object owner, String masthead, String message, Throwable exception) {
		Platform.runLater(() -> {
			String title = "Exception";
			
			Alert alert = new Alert(AlertType.ERROR);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(getActlistIcons());
			alert.setTitle(title);
			alert.setHeaderText(masthead);
			alert.setContentText(message);
			
			if (owner == null) {
				ObservableList<Stage> stages = StageHelper.getStages();
				if (stages.isEmpty() == false) {
					alert.initOwner(stages.get(0));
				}
			} else {
				alert.initOwner((Window) owner);
			}
			
			// Create expandable Exception.
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			String exceptionText = sw.toString();

			Label label = new Label("The exception stacktrace was:");

			TextArea textArea = new TextArea(exceptionText);
			textArea.setEditable(false);
			textArea.setWrapText(true);

			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);

			GridPane expContent = new GridPane();
			expContent.setMaxWidth(Double.MAX_VALUE);
			expContent.add(label, 0, 0);
			expContent.add(textArea, 0, 1);

			alert.getDialogPane().setExpandableContent(expContent);
			
			alert.showAndWait();
		});
	}
}
