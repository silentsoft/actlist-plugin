package org.silentsoft.actlist.plugin;

import java.awt.Desktop;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.controlsfx.control.PopOver;
import org.silentsoft.actlist.plugin.ActlistPlugin.Function;
import org.silentsoft.actlist.plugin.ActlistPlugin.SupportedPlatform;
import org.silentsoft.actlist.plugin.messagebox.MessageBox;
import org.silentsoft.actlist.plugin.tray.TrayNotification;
import org.silentsoft.core.util.DateUtil;
import org.silentsoft.core.util.FileUtil;
import org.silentsoft.core.util.JSONUtil;
import org.silentsoft.core.util.ObjectUtil;
import org.silentsoft.core.util.SystemUtil;

import com.github.markusbernhardt.proxy.ProxySearch;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXToggleButton;

import de.codecentric.centerdevice.glass.AdapterContext;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import tray.animations.AnimationType;

/**
 * <em><tt>WARNING : This Debug application's source code is independent with real Actlist application's source code. So this application might not be same with real Actlist application.</tt></em></p>
 * 
 * This class is designed for debugging the Actlist plugin.</p>
 * </p>
 * 
 * @author silentsoft
 */
public final class DebugApp extends Application {

	static boolean isDebugMode = false;
	static String proxyHost = null;
	
	public static void debug() {
		debug(null);
	}
	
	/**
	 * @param proxyHost e.g. "http://1.2.3.4:8080"
	 */
	public static void debug(String proxyHost) {
		DebugApp.isDebugMode = true;
		DebugApp.proxyHost   = proxyHost;
		
		launch("");
	}
	
	Stage stage;
	
	ActlistPlugin plugin;
	
	PopOver popOver;
	
	ObservableList<Node> functions;
	
	HashMap<org.silentsoft.actlist.plugin.tray.TrayNotification, tray.notification.TrayNotification> trayNotifications = new HashMap<org.silentsoft.actlist.plugin.tray.TrayNotification, tray.notification.TrayNotification>();
	
	boolean isAvailableNewPlugin = false;
	URI newPluginURI;

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		
		String mainClass = null;
		Class<?> pluginClass = null;
		InputStream inputStream = null;
		
		try {
			inputStream = new FileInputStream(Paths.get(System.getProperty("user.dir"), "target", "classes", "META-INF", "MANIFEST.MF").toString());
			
			Manifest manifest = new Manifest(inputStream);
			mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS).trim();
			if (ObjectUtil.isEmpty(mainClass)) {
				mainClass = "Plugin";
			}
		} catch (Exception | Error e) {
			e.printStackTrace();
		} finally {
			try {
				pluginClass = getClass().getClassLoader().loadClass(mainClass);
			} catch (ClassNotFoundException e) {
				StringBuffer message = new StringBuffer();
				message.append(String.join("", "[ERROR] '", mainClass, "' class is not exists. Please check 'mainClass' property in pom.xml", "\r\n"));
				message.append(String.join("", ">>", "\r\n"));
				message.append(String.join("", "    <properties>", "\r\n"));
				message.append(String.join("", "        <mainClass>your.pkg.Plugin</mainClass>", "\r\n"));
				message.append(String.join("", "    </properties>", "\r\n"));
				message.append(String.join("", "<<", "\r\n"));
				System.err.println(message.toString());
			}
			
			if (inputStream != null) {
				inputStream.close();
			}
		}
		
		/* source mirroring from real Actlist application */
		
		AtomicBoolean shouldTraceException = new AtomicBoolean(true);
		if (ActlistPlugin.class.isAssignableFrom(pluginClass)) {
			this.plugin = ActlistPlugin.class.cast(pluginClass.newInstance());
			
			popOver = new PopOver(new VBox());
			((VBox) popOver.getContentNode()).setPadding(new Insets(3, 3, 3, 3));
			popOver.setArrowLocation(PopOver.ArrowLocation.TOP_LEFT);
			
			SupportedPlatform currentPlatform = null;
			{
				if (SystemUtil.isWindows()) {
					currentPlatform = SupportedPlatform.WINDOWS;
				} else if (SystemUtil.isMac()) {
					currentPlatform = SupportedPlatform.MACOSX;
				} /* else if (SystemUtil.isLinux()) {
					currentPlatform = SupportedPlatform.LINUX;
				} else {
					currentPlatform = SupportedPlatform.UNKNOWN;
				} */
			}
			plugin.currentPlatformObject().set(currentPlatform);
			
			SupportedPlatform[] supportedPlatforms = plugin.getSupportedPlatforms();
			if (supportedPlatforms != null) {
				if (supportedPlatforms.length > 0 && Arrays.asList(supportedPlatforms).contains(currentPlatform) == false) {
					shouldTraceException.set(false);
					
					List<String> listOfSupportedPlatform = Arrays.stream(supportedPlatforms).map(Enum::name).collect(Collectors.toList());
					String errorMessage = String.join("", "This plugin only supports ", String.join(", ", listOfSupportedPlatform));
					Runnable errorDialog = () -> {
						Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("Error");
						alert.setHeaderText("Unsupported platform");
						alert.setContentText(errorMessage);
						alert.showAndWait();
					};
					Label lblPluginName = (Label) stage.getScene().lookup("#lblPluginName");
					lblPluginName.setOnMouseClicked(mouseEvent -> {
						if (mouseEvent.getButton() == MouseButton.PRIMARY) {
							errorDialog.run();
						}
					});
					Label warningLabel = (Label) stage.getScene().lookup("#warningLabel");
					warningLabel.setOnMouseClicked(mouseEvent -> {
						errorDialog.run();
					});
					warningLabel.setVisible(true);
					playFadeTransition(warningLabel);
					
					throw new Exception(errorMessage);
				}
			}
			
			/* skip minimum compatible version */
			
			plugin.classLoaderObject().set(getClass().getClassLoader());
			
			plugin.proxyHostObject().set(RESTfulAPI.getProxyHost());
			
			plugin.setPluginConfig(new PluginConfig("debug"));
			File configFile = Paths.get(System.getProperty("user.dir"), "plugins", "config", "debug.config").toFile();
			if (configFile.exists()) {
				String configContent = FileUtil.readFile(configFile);
				PluginConfig pluginConfig = JSONUtil.JSONToObject(configContent, PluginConfig.class);
				if (pluginConfig != null) {
					plugin.setPluginConfig(pluginConfig);
				}
			}
			plugin.shouldShowLoadingBar().addListener((observable, oldValue , newValue) -> {
				if (oldValue == newValue) {
					return;
				}
				
				displayLoadingBar(newValue);
			});
			plugin.exceptionObject().addListener((observable, oldValue, newValue) -> {
				if (newValue != null) {
					makeDisable(newValue, true);
					
					plugin.exceptionObject().set(null);
				}
			});
			plugin.showTrayNotificationObject().addListener((observable, oldValue, newValue) -> {
				if (newValue != null) {
					tray.notification.TrayNotification trayNotification = new tray.notification.TrayNotification();
					
					synchronized (trayNotifications) {
						trayNotifications.put(newValue, trayNotification);
					}
					
					trayNotification.setRectangleFill(Paint.valueOf("#222222"));
					trayNotification.setImage(new Image("/images/icon/actlist_128.png"));
					trayNotification.setAnimationType(AnimationType.POPUP);
					
					String titleValue = (plugin.getPluginName() == null || plugin.getPluginName().trim().isEmpty()) ? "(empty name)" : plugin.getPluginName();
					String titlePrefix = String.format("[%s] ", titleValue);
					if (newValue.getTitle() == null || newValue.getTitle().trim().isEmpty()) {
						trayNotification.setTitle(titlePrefix.concat(""));
					} else {
						trayNotification.setTitle(titlePrefix.concat(newValue.getTitle()));
					}
					
					if (newValue.getMessage() == null || newValue.getMessage().trim().isEmpty()) {
						trayNotification.setMessage("(empty message)");
					} else {
						trayNotification.setMessage(newValue.getMessage());
					}
					
					trayNotification.setOnDismiss((actionEvent) -> {
						synchronized (trayNotifications) {
							trayNotifications.remove(newValue);
						}
						
						if (newValue.getDuration() == null) {
							bringToFront();
							/*AnimationUtils.createTransition(lblPluginName, jidefx.animation.AnimationType.FLASH).play();*/
							// TODO : scrollTo
						}
					});
					
					if (newValue.getDuration() == null) {
						trayNotification.showAndWait();
					} else {
						trayNotification.showAndDismiss(newValue.getDuration());
					}
					
					plugin.showTrayNotificationObject().set(null);
				}
			});
			{
				Consumer<tray.notification.TrayNotification> dismiss = (trayNotification) -> {
					new Thread(() -> {
						while (trayNotification.isTrayShowing() == false) {
							try {
								Thread.sleep(500);
							} catch (Exception e) {
								
							}
						}
						Platform.runLater(() -> {
							trayNotification.dismiss();
						});
					}).start();
				};
				plugin.dismissTrayNotificationObject().addListener((observable, oldValue, newValue) -> {
					if (newValue != null) {
						synchronized (trayNotifications) {
							if (trayNotifications.containsKey(newValue)) {
								tray.notification.TrayNotification trayNotification = trayNotifications.get(newValue);
								dismiss.accept(trayNotification);
							}
						}
						
						plugin.dismissTrayNotificationObject().set(null);
					}
				});
				plugin.shouldDismissTrayNotifications().addListener((observable, oldValue, newValue) -> {
					if (newValue) {
						synchronized (trayNotifications) {
							for (Entry<TrayNotification, tray.notification.TrayNotification> entrySet : trayNotifications.entrySet()) {
								tray.notification.TrayNotification trayNotification = entrySet.getValue();
								dismiss.accept(trayNotification);
							}
							
							plugin.shouldDismissTrayNotifications().set(false);
						}
					}
				});
			}
			plugin.shouldBrowseActlistArchives().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					try {
						Desktop.getDesktop().browse(new URI("http://actlist.silentsoft.org/archives/"));
					} catch (Exception e) {
						
					}
					
					plugin.shouldBrowseActlistArchives().set(false);
				}
			});
			plugin.shouldRequestShowActlist().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					if (isHidden()) {
						// request show
						showOrHide();
					}
				} else {
					if (isShown()) {
						// request hide
						showOrHide();
					}
				}
			});
			plugin.shouldRequestDeactivate().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					JFXToggleButton toggle = (JFXToggleButton) stage.getScene().lookup("#togActivator");
					boolean isActivated = toggle.selectedProperty().get();
					if (isActivated) {
						toggle.setSelected(false);
						deactivated();
					}
					
					plugin.shouldRequestDeactivate().set(false);
				}
			});
			
			AnchorPane root = new AnchorPane();
			root.setPrefWidth(435.0);
			root.setStyle("-fx-background-color: #ffffff;");
			root.getChildren().add(createHamburger());
			root.getChildren().add(createHead());
			root.getChildren().add(createToggleBox());
			root.getChildren().add(createSeparator());
			root.getChildren().add(createContentBox());
			root.getChildren().add(createContentLoadingBox());
			
			stage.setScene(new Scene(root));
			
			stage.setTitle("Actlist Debug App");
			stage.show();
			
			/**
			 * Exception will raised when if control their graphic node on initialize() method.
			 * so, need to mapping controller to plugin before call initialize method.
			 * below getGraphic() method will mapping controller to plugin.
			 */
			if (plugin.existsGraphic()) {
				plugin.getGraphic();
			}
			
			plugin.initialize();
			
			functions = FXCollections.observableArrayList();
			for (Function function : plugin.getFunctionMap().values()) {
				addFunction(function);
			}
			
			if (plugin.isOneTimePlugin() == false) {
				activated();
			}
			
			/* finally */
			
			new Thread(() -> {
				Runnable checkUpdate = () -> {
					Label warningLabel = (Label) stage.getScene().lookup("#warningLabel");
					Label updateAlarmLabel = (Label) stage.getScene().lookup("#updateAlarmLabel");
					
					try {
						URI pluginUpdateCheckURI = plugin.getPluginUpdateCheckURI();
						if (pluginUpdateCheckURI != null) {
							Map<String, Object> result = null;
							
							ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
			    			param.add(new BasicNameValuePair("version", plugin.getPluginVersion()));
			    			/* below values are unnecessary. version value is enough.
			    			param.add(new BasicNameValuePair("os", SystemUtil.getOSName()));
			    			param.add(new BasicNameValuePair("architecture", SystemUtil.getPlatformArchitecture()));
			    			*/
							
							String uri = pluginUpdateCheckURI.toString();
							if (uri.matches("(?i).*\\.js")) {
								StringBuffer script = new StringBuffer();
								script.append(String.format("var version = '%s';", plugin.getPluginVersion())).append("\r\n");
								script.append(RESTfulAPI.doGet(pluginUpdateCheckURI.toString(), param, String.class));
								
								ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
								Object _result = scriptEngine.eval(script.toString());
								if (_result instanceof Map) {
									result = (Map) _result;
								}
							} else {
				    			result = RESTfulAPI.doGet(pluginUpdateCheckURI.toString(), param, Map.class);
							}
			    			
			    			if (result == null) {
			    				return;
			    			}
			    			
			    			if (result.containsKey("available")) {
			    				isAvailableNewPlugin = Boolean.parseBoolean(String.valueOf(result.get("available")));
			    				if (isAvailableNewPlugin) {
			    					if (result.containsKey("url")) {
			    						try {
			    							newPluginURI = new URI(String.valueOf(result.get("url")));
			        					} catch (Exception e) {
			        						e.printStackTrace();
			        					}
			    					}
			    					
			    					try {
			    						plugin.pluginUpdateFound();
			    					} catch (Exception e) {
			    						e.printStackTrace();
			    					}

			    					URI pluginArchivesURI = plugin.getPluginArchivesURI();
		    						if (pluginArchivesURI != null) {
		    							newPluginURI = pluginArchivesURI;
		    						}
			    					
			    					if (newPluginURI != null) {
			    						updateAlarmLabel.setVisible(true);
			    						playFadeTransition(updateAlarmLabel);
			    					} else {
			    						updateAlarmLabel.setVisible(false);
			    					}
			    				}
			    			}
			    			
			    			if (result.containsKey("killSwitch")) {
			    				boolean hasTurnedOnKillSwitch = "on".equalsIgnoreCase(String.valueOf(result.get("killSwitch")).trim());
			    				if (hasTurnedOnKillSwitch) {
			    					String message = "The plugin's kill switch has turned on by the author.";
			    					
			    					makeDisable(new Exception(message), false);
			    					
			    					warningLabel.setOnMouseClicked(mouseEvent -> {
										MessageBox.showInformation(stage, message);
									});
									warningLabel.setVisible(true);
									playFadeTransition(warningLabel);
			    				}
			    			}
			    			
			    			if (result.containsKey("endOfService")) {
			    				boolean hasEndOfService = Boolean.parseBoolean(String.valueOf(result.get("endOfService")));
								if (hasEndOfService) {
									warningLabel.setOnMouseClicked(mouseEvent -> {
										MessageBox.showInformation(stage, "This plugin has reached end of service by the author.");
									});
									warningLabel.setVisible(true);
									playFadeTransition(warningLabel);
								}
			    			}
						}
					} catch (Exception e) {
						e.printStackTrace(); // print stack trace only ! do nothing ! b/c of its not kind of critical exception.
					}
				};
				
				boolean shouldCheck = true;
				Date latestCheckDate= null;
				while (true) {
					if (shouldCheck) {
						checkUpdate.run();
						latestCheckDate = Calendar.getInstance().getTime();
					}
					try {
						Thread.sleep((long)Duration.minutes(10).toMillis());
					} catch (InterruptedException ie) {
						
					} finally {
						shouldCheck = DateUtil.getDifferenceHoursFromNow(latestCheckDate) >= 24;
					}
				}
			}).start();
		} else {
			throw new Exception("The Plugin class must be extends ActlistPlugin !");
		}
	}
	
	private void showOrHide() {
		if (stage.isIconified()) {
			Platform.runLater(() -> { stage.setIconified(false); }); // just bring it up to front from taskbar.
		} else {
			if (stage.isShowing()) {
				if (stage.isFocused()) {
//					if (ConfigUtil.isAnimationEffect()) {
//						Transition animation = AnimationUtils.createTransition(app, AnimationType.BOUNCE_OUT_DOWN);
//		    			animation.setOnFinished(actionEvent -> {
//		    				stage.hide();
//		    			});
//		    			animation.play();
//					} else {
					if (SystemUtil.isMac()) {
						AdapterContext.getContext().getApplicationAdapter().hide();
					} else {
						Platform.runLater(() -> { stage.hide(); });
					}
//					}
				} else {
					if (SystemUtil.isMac()) {
						Platform.runLater(() -> {
							stage.toFront();

							/* WHAT THE HECK .. ! BUT THIS IS NECESSARY BECAUSE OF stage.requestFocus() DOES NOT WORKS PROPERLY ON MAC */
							{
								Point previousMouseLocation = MouseInfo.getPointerInfo().getLocation();
								new Thread(() -> {
									Platform.runLater(() -> {
										try {
											Robot robot = new Robot();
											robot.mouseMove((int) stage.getX()+10, (int) stage.getY()+10);
											robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
											robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
											
											robot.mouseMove(previousMouseLocation.x, previousMouseLocation.y);
										} catch (Exception e) {
											
										}
									});
								}).start();
							}
						});
					} else {
						Platform.runLater(() -> { stage.requestFocus(); }); // do not hide. just bring it up to front.
					}
				}
			} else {
//				if (ConfigUtil.isAnimationEffect()) {
//					AnimationUtils.createTransition(app, AnimationType.BOUNCE_IN).play();
//				}
				if (SystemUtil.isMac()) {
					AdapterContext.getContext().getApplicationAdapter().unhideAllApplications();
				} else {
					Platform.runLater(() -> { stage.show(); });
				}
			}
		}
	}
	
	private void bringToFront() {
		if (isHidden()) {
			showOrHide(); // in this case the Actlist will definitely showing up.
		}
	}
	
	private boolean isShown() {
		return !isHidden();
	}
	
	private boolean isHidden() {
		if (stage.isIconified() ||
			stage.isShowing() == false ||
		    (stage.isShowing() == true && stage.isFocused() == false)) {
			return true;
		}
		
		return false;
	}
	
	private void playFadeTransition(Node node) {
		Runnable action = () -> {
			if (node.isVisible()) {
				FadeTransition fadeTransition = new FadeTransition(Duration.millis(400), node);
				fadeTransition.setFromValue(1.0);
				fadeTransition.setToValue(0.3);
				fadeTransition.setCycleCount(6);
				fadeTransition.setAutoReverse(true);
				 
				fadeTransition.play();
			}
		};
		if (Platform.isFxApplicationThread()) {
			action.run();
		} else {
			Platform.runLater(() -> {
				action.run();
			});
		}
	}
	
	private void makeDisable(Throwable throwable, boolean shouldTraceException) {
		new Thread(() -> {
			JFXToggleButton togActivator = (JFXToggleButton) stage.getScene().lookup("#togActivator");
			if (togActivator.selectedProperty().get()) {
				try {
					// wait for animation to the end.
					Thread.sleep(100);
				} catch (Exception e) {
					
				}
			}
			
			Platform.runLater(() -> {
				Label lblPluginName = (Label) stage.getScene().lookup("#lblPluginName");
				lblPluginName.setCursor(Cursor.HAND);
				
				if (shouldTraceException) {
					Runnable exceptionDialog = () -> {
						Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("Exception Dialog");
						alert.setHeaderText(plugin.getPluginName());

						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						throwable.printStackTrace(pw);
						String exceptionText = sw.toString();

						TextArea textArea = new TextArea(exceptionText);
						textArea.setEditable(false);
						textArea.setWrapText(true);

						textArea.setMaxWidth(Double.MAX_VALUE);
						textArea.setMaxHeight(Double.MAX_VALUE);
						GridPane.setVgrow(textArea, Priority.ALWAYS);
						GridPane.setHgrow(textArea, Priority.ALWAYS);

						GridPane content = new GridPane();
						content.setMaxWidth(Double.MAX_VALUE);
						content.add(textArea, 0, 0);

						alert.getDialogPane().setContent(content);

						alert.showAndWait();
					};
					
					lblPluginName.setTooltip(new Tooltip("Click to show the exception log."));
					lblPluginName.setOnMouseClicked(mouseEvent -> {
						if (mouseEvent.getButton() == MouseButton.PRIMARY) {
							exceptionDialog.run();
						}
					});
					
					Label warningLabel = (Label) stage.getScene().lookup("#warningLabel");
					warningLabel.setOnMouseClicked(mouseEvent -> {
						exceptionDialog.run();
					});
					warningLabel.setVisible(true);
					playFadeTransition(warningLabel);
				} else {
					lblPluginName.setTooltip(new Tooltip(throwable.getMessage()));
				}
				
				togActivator.setUnToggleLineColor(Paint.valueOf("#da4242"));
				togActivator.setDisable(true);
				togActivator.setOpacity(1.0); // remove disable effect.
				togActivator.setSelected(false);
				
				clearPluginGraphic();
			});
		}).start();
	}
	
	private void addFunction(Function function) {
		functions.add(createFunctionBox(new Label("", function.graphic), mouseEvent -> {
			try {
				if (function.action != null) {
					function.action.run();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				popOver.hide();
			}
		}));
	}
	
	private HBox createFunctionBox(Node node, javafx.event.EventHandler<? super MouseEvent> action) {
		HBox hBox = new HBox(node);
		hBox.setAlignment(Pos.CENTER);
		hBox.setPadding(new Insets(3, 3, 3, 3));
		hBox.setStyle("-fx-background-color: white;");
		hBox.setOnMouseEntered(mouseEvent -> {
			hBox.setStyle("-fx-background-color: lightgray;");
		});
		hBox.setOnMouseExited(mouseEvent -> {
			hBox.setStyle("-fx-background-color: white;");
		});
		hBox.addEventFilter(MouseEvent.MOUSE_CLICKED, action);
		
		return hBox;
	}
	
	Stage aboutStage;
	private HBox createAboutFunction() {
		return createFunctionBox(new Label("About"), mouseEvent -> {
			stage.getScene().lookup("#updateAlarmLabel").getOnMouseClicked().handle(null);
		});
	}
	
	private JFXHamburger createHamburger() {
		JFXHamburger hamburger = new JFXHamburger();
		hamburger.setLayoutX(13.0);
		hamburger.setLayoutY(19.0);
		hamburger.setOpacity(0.2);
		hamburger.setPrefHeight(14.0);
		hamburger.setPrefWidth(11.0);
		hamburger.setCursor(Cursor.MOVE);
		
		return hamburger;
	}
	
	private HBox createHead() {
		Label lblPluginName = new Label();
		lblPluginName.setId("lblPluginName");
		lblPluginName.setText((plugin.getPluginName() == null || plugin.getPluginName().trim().isEmpty()) ? "(empty name)" : plugin.getPluginName());
		lblPluginName.setPrefHeight(16.0);
		HBox.setHgrow(lblPluginName, Priority.ALWAYS);
		lblPluginName.setFont(Font.font("Arial", 14.0));
		
		String pluginDescription = plugin.getPluginDescription();
		if (ObjectUtil.isNotEmpty(pluginDescription)) {
			lblPluginName.setTooltip(new Tooltip(pluginDescription));
		}
		
		HBox head = new HBox(lblPluginName);
		head.setAlignment(Pos.CENTER_LEFT);
		head.setPrefHeight(45.0);
		AnchorPane.setLeftAnchor(head, 35.0);
		AnchorPane.setRightAnchor(head, 102.0);
		AnchorPane.setTopAnchor(head, 0.0);
		head.setOpaqueInsets(Insets.EMPTY);
		head.setPadding(new Insets(5.0, 0.0, 0.0, 0.0));
		head.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				if (popOver != null) {
					((VBox) popOver.getContentNode()).getChildren().clear();
					
					((VBox) popOver.getContentNode()).getChildren().add(createAboutFunction());
					
					JFXToggleButton toggle = (JFXToggleButton) stage.getScene().lookup("#togActivator");
					if (toggle.selectedProperty().get()) {
						if (plugin.getFunctionMap().size() > 0) {
							((VBox) popOver.getContentNode()).getChildren().add(new Separator(Orientation.HORIZONTAL));
						}
						
						((VBox) popOver.getContentNode()).getChildren().addAll(functions);
					}
					
					popOver.show(stage.getScene().lookup("#contentLoadingBox"), e.getScreenX(), e.getScreenY());
				}
			}
		});
		
		return head;
	}
	
	private HBox createToggleBox() {
		Label warningLabel = new Label();
		warningLabel.setId("warningLabel");
		warningLabel.setMaxHeight(6.0);
		warningLabel.setMaxWidth(6.0);
		warningLabel.setMinHeight(6.0);
		warningLabel.setMinWidth(6.0);
		warningLabel.setOnMouseClicked(mouseEvent -> {
			warningLabel.setVisible(false);
			
			try {
				String warningText = plugin.getWarningText();
				if (ObjectUtil.isNotEmpty(warningText)) {
					MessageBox.showWarning(stage, warningText);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		warningLabel.setStyle("-fx-background-color: orange; -fx-background-radius: 5em;");
		warningLabel.setVisible(false);
		warningLabel.setCursor(Cursor.HAND);
		
		Label updateAlarmLabel = new Label();
		updateAlarmLabel.setId("updateAlarmLabel");
		updateAlarmLabel.setMaxHeight(6.0);
		updateAlarmLabel.setMaxWidth(6.0);
		updateAlarmLabel.setMinHeight(6.0);
		updateAlarmLabel.setMinWidth(6.0);
		updateAlarmLabel.setOnMouseClicked(mouseEvent -> {
			updateAlarmLabel.setVisible(false);
			
			/**
			 * this aboutStage must be closed when if already opened.
			 * because the newPluginURI variable will be set by another thread.
			 */
			if (aboutStage != null) {
				aboutStage.close();
				aboutStage = null;
			}
			
			aboutStage = new Stage();
			aboutStage.initOwner(this.stage);
			aboutStage.initStyle(StageStyle.UTILITY);
			{
				ImageView iconImage = new ImageView();
				iconImage.setFitHeight(48.0);
				iconImage.setFitWidth(48.0);
				iconImage.setPickOnBounds(true);
				iconImage.setPreserveRatio(true);
				iconImage.setImage(new Image("/images/icon/actlist_48.png"));
				
				VBox iconBox = new VBox(iconImage);
				iconBox.setAlignment(Pos.TOP_CENTER);
				iconBox.setPadding(new Insets(0.0, 0.0, 10.0, 0.0));
				
				Label name = new Label();
				name.setFont(Font.font(23.0));
				
				Label version = new Label();
				Label authorText = new Label();
				Hyperlink authorLink = new Hyperlink();
				authorLink.setFocusTraversable(false);
				
				HBox versionAndAuthorBox = new HBox(version, authorText, authorLink);
				versionAndAuthorBox.setAlignment(Pos.CENTER);
				
				SVGPath svg = new SVGPath();
				svg.setContent("M23 12l-2.44-2.78.34-3.68-3.61-.82-1.89-3.18L12 3 8.6 1.54 6.71 4.72l-3.61.81.34 3.68L1 12l2.44 2.78-.34 3.69 3.61.82 1.89 3.18L12 21l3.4 1.46 1.89-3.18 3.61-.82-.34-3.68L23 12zm-10 5h-2v-2h2v2zm0-4h-2V7h2v6z");
				svg.setFill(Paint.valueOf("#4d4d4d"));
				
				Hyperlink newVersionLink = new Hyperlink();
				newVersionLink.setFocusTraversable(false);
				newVersionLink.setStyle("-fx-padding: 0;");
				newVersionLink.setText("New version available");
				newVersionLink.setTextFill(Paint.valueOf("#ee7676"));
				
				HBox newVersionBox = new HBox(svg, newVersionLink);
				newVersionBox.setAlignment(Pos.CENTER);
				newVersionBox.setPrefHeight(100.0);
				newVersionBox.setPrefWidth(200.0);
				newVersionBox.setSpacing(3.0);
				newVersionBox.setVisible(false);
				
				VBox contentBox = new VBox(name, versionAndAuthorBox, newVersionBox);
				contentBox.setAlignment(Pos.TOP_CENTER);
				
				VBox child = new VBox(iconBox, contentBox);
				child.setPrefHeight(75.0);
				
				VBox rootVBox = new VBox(child);
				rootVBox.setLayoutX(106.0);
				rootVBox.setLayoutY(63.0);
				rootVBox.setSpacing(7.0);
				AnchorPane.setBottomAnchor(rootVBox, 15.0);
				AnchorPane.setLeftAnchor(rootVBox, 15.0);
				AnchorPane.setRightAnchor(rootVBox, 15.0);
				AnchorPane.setTopAnchor(rootVBox, 15.0);
				
				AnchorPane rootPane = new AnchorPane(rootVBox);
				rootPane.setMinWidth(360.0);
				rootPane.setStyle("-fx-background-color: white;");
				
				aboutStage.setScene(new Scene(new BorderPane(rootPane)));
				
				{
					if (plugin.existsIcon()) {
						try {
							iconImage.setImage(plugin.getIcon().getImage());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					if (ObjectUtil.isNotEmpty(plugin.getPluginName())) {
						name.setText(plugin.getPluginName());
					}
					
					if (ObjectUtil.isNotEmpty(plugin.getPluginVersion())) {
						version.setText(plugin.getPluginVersion());
					}
					
					if (ObjectUtil.isNotEmpty(plugin.getPluginAuthor())) {
						if (ObjectUtil.isNotEmpty(plugin.getPluginAuthorURI())) {
							authorLink.setText(plugin.getPluginAuthor());
							authorLink.setOnAction(actionEvent -> {
								authorLink.setVisited(false);
								
								try {
									Desktop.getDesktop().browse(plugin.getPluginAuthorURI());
								} catch (Exception e) {
									e.printStackTrace();
								}
							});
						} else {
							authorText.setText(" by ".concat(plugin.getPluginAuthor()));
						}
					}
					
					{
						if (isAvailableNewPlugin && newPluginURI != null) {
							newVersionBox.setVisible(true);
							
							newVersionLink.setOnAction(actionEvent -> {
								newVersionLink.setVisited(false);
								
								try {
									Desktop.getDesktop().browse(newPluginURI);
								} catch (Exception e) {
									e.printStackTrace();
								}
							});
						} else {
							newVersionBox.setVisible(false);
							
							VBox parent = (VBox) newVersionBox.getParent();
							parent.getChildren().remove(newVersionBox);
						}
					}
					
					{
						Supplier<TabPane> createContentTabPane = () -> {
							TabPane tabPane = new TabPane();
							tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
							
							ScrollPane scrollPane = new ScrollPane(tabPane);
							scrollPane.setFitToWidth(true);
							scrollPane.setFitToHeight(true);
							scrollPane.setPrefWidth(330.0);
							scrollPane.setPrefHeight(310.0);
							
							VBox.setVgrow(scrollPane, Priority.ALWAYS);
							
							rootVBox.getChildren().add(scrollPane);
							
							return tabPane;
						};
						
						Consumer<Object[]> makeTabIfContentExists = (objects) -> {
							TabPane tabPane = (TabPane) objects[0];
							String title = (String) objects[1];
							URI uri = (URI) objects[2];
							String text = (String) objects[3];
							
							boolean existsContent = ObjectUtil.isNotEmpty(uri) || ObjectUtil.isNotEmpty(text);
							if (existsContent) {
								WebView webView = new WebView();
								{
									BufferedReader reader = null;
									try {
										if (ObjectUtil.isNotEmpty(uri)) {
											if ("jar".equals(uri.getScheme())) {
												reader = new BufferedReader(new InputStreamReader(uri.toURL().openStream(), Charset.forName("UTF-8")));
												StringBuffer buffer = new StringBuffer();
												for (String value=null; (value=reader.readLine()) != null; ) {
													buffer.append(value.concat("\r\n"));
												}
												webView.getEngine().loadContent(buffer.toString(), "text/plain");
											} else {
												webView.getEngine().load(uri.toString());
											}
										} else if (ObjectUtil.isNotEmpty(text)) {
											webView.getEngine().loadContent(text, "text/plain");
										}
									} catch (Exception e) {
										e.printStackTrace();
									} finally {
										if (reader != null) {
											try {
												reader.close();
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
									}
								}
								tabPane.getTabs().add(new Tab(title, webView));
							}
						};
						
						BooleanSupplier haveContentToShow = () -> {
							boolean result = false;
							if (ObjectUtil.isNotEmpty(plugin.getPluginDescriptionURI()) || ObjectUtil.isNotEmpty(plugin.getPluginDescription())) {
								result = true;
							} else if (ObjectUtil.isNotEmpty(plugin.getPluginChangeLogURI()) || ObjectUtil.isNotEmpty(plugin.getPluginChangeLog())) {
								result = true;
							} else if (ObjectUtil.isNotEmpty(plugin.getPluginLicenseURI()) || ObjectUtil.isNotEmpty(plugin.getPluginLicense())) {
								result = true;
							}
							return result;
						};
						if (haveContentToShow.getAsBoolean()) {
							TabPane tabPane = createContentTabPane.get();
							makeTabIfContentExists.accept(new Object[] {tabPane, "Description", plugin.getPluginDescriptionURI(), plugin.getPluginDescription()} );
							makeTabIfContentExists.accept(new Object[] {tabPane, "Change Log", plugin.getPluginChangeLogURI(), plugin.getPluginChangeLog()} );
							makeTabIfContentExists.accept(new Object[] {tabPane, "License", plugin.getPluginLicenseURI(), plugin.getPluginLicense()} );
							aboutStage.getScene().getWindow().sizeToScene();
						}
					}
				}
			}
			aboutStage.show();
		});
		updateAlarmLabel.setStyle("-fx-background-color: red; -fx-background-radius: 5em;");
		updateAlarmLabel.setVisible(false);
		updateAlarmLabel.setCursor(Cursor.HAND);
		
		HBox innerBox = new HBox(warningLabel, updateAlarmLabel);
		innerBox.setAlignment(Pos.CENTER);
		innerBox.setSpacing(8.0);
		HBox.setMargin(innerBox, Insets.EMPTY);
		
		JFXToggleButton togActivator = new JFXToggleButton();
		togActivator.setId("togActivator");
		togActivator.setText(" ");
		togActivator.setSelected(false);
		togActivator.setToggleColor(Paint.valueOf("#fafafa"));
		togActivator.setToggleLineColor(Paint.valueOf("#59bf53"));
		togActivator.setUnToggleLineColor(Paint.valueOf("#e0e0e0"));
		togActivator.setOnAction(actionEvent -> {
			if (isActivated()) {
				activated();
			} else {
				deactivated();
			}
		});
		
		String warningText = plugin.getWarningText();
		if (ObjectUtil.isNotEmpty(warningText)) {
			warningLabel.setVisible(true);
			playFadeTransition(warningLabel);
		} else {
			warningLabel.setVisible(false);
		}
		
		if (plugin.isOneTimePlugin()) {
			togActivator.setSelected(false);
		} else {
			togActivator.setSelected(true);
		}
		
		HBox toggleBox = new HBox(innerBox, togActivator);
		toggleBox.setAlignment(Pos.CENTER_LEFT);
		toggleBox.setLayoutX(361.0);
		toggleBox.setLayoutY(-2.0);
		toggleBox.setSpacing(8.0);
		AnchorPane.setRightAnchor(toggleBox, 0.0);
		
		return toggleBox;
	}
	
	private Separator createSeparator() {
		Separator separator = new Separator();
		separator.setLayoutX(35.0);
		separator.setLayoutY(50.0);
		separator.setPrefWidth(215.0);
		AnchorPane.setLeftAnchor(separator, 35.0);
		AnchorPane.setRightAnchor(separator, 20.0);
		
		return separator;
	}
	
	private VBox createContentBox() {
		VBox contentBox = new VBox();
		contentBox.setId("contentBox");
		contentBox.setLayoutX(35.0);
		contentBox.setLayoutY(51.0);
		contentBox.setPrefWidth(380.0);
		AnchorPane.setRightAnchor(contentBox, 20.0);
		AnchorPane.setLeftAnchor(contentBox, 35.0);
		
		return contentBox;
	}
	
	private VBox createContentLoadingBox() {
		VBox contentLoadingBox = new VBox();
		contentLoadingBox.setId("contentLoadingBox");
		contentLoadingBox.setAlignment(Pos.CENTER);
		contentLoadingBox.setLayoutX(35.0);
		contentLoadingBox.setLayoutY(51.0);
		contentLoadingBox.setPrefWidth(380.0);
		contentLoadingBox.setStyle("-fx-background-color: white;");
		contentLoadingBox.setVisible(false);
		AnchorPane.setBottomAnchor(contentLoadingBox, 3.0);
		AnchorPane.setLeftAnchor(contentLoadingBox, 35.0);
		AnchorPane.setRightAnchor(contentLoadingBox, 0.0);
		AnchorPane.setTopAnchor(contentLoadingBox, 51.0);
		
		return contentLoadingBox;
	}
	
	private void displayLoadingBar(boolean shouldShowLoadingBar) {
		if (plugin.existsGraphic()) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					VBox contentLoadingBox = (VBox) stage.getScene().lookup("#contentLoadingBox");
					
					contentLoadingBox.getChildren().clear();
					
					if (shouldShowLoadingBar) {
						contentLoadingBox.getChildren().add(new JFXSpinner());
					}
					
					contentLoadingBox.setVisible(shouldShowLoadingBar);
				}
			};
			
			if (Platform.isFxApplicationThread()) {
				runnable.run();
			} else {
				Platform.runLater(() -> {
					runnable.run();
				});
			}
		}
	}
	
	private void loadPluginGraphic() {
		/*
		  <VBox fx:id="contentBox" layoutX="35.0" layoutY="50.0" prefWidth="380.0" AnchorPane.leftAnchor="35.0" AnchorPane.rightAnchor="20.0">
		     <children>
		        <!-- Generate by code. 
		        <BorderPane fx:id="contentPane" />
		        <Separator prefWidth="215.0">
		           <padding>
		              <Insets top="5.0" />
		           </padding>
		        </Separator>
		        -->
		     </children>
		  </VBox>
		  <VBox fx:id="contentLoadingBox" visible="false" alignment="CENTER" layoutX="35.0" layoutY="50.0" prefWidth="380.0" style="-fx-background-color: white;" AnchorPane.leftAnchor="35.0" AnchorPane.rightAnchor="0.0">
		     <children>
		        <!-- Generate by code. 
		        <JFXSpinner />
		        -->
		     </children>
		  </VBox>
		 */
		
		try {
			if (plugin.existsGraphic()) {
				Node pluginContent = plugin.getGraphic();
				if (pluginContent != null) {
					VBox contentBox = (VBox) stage.getScene().lookup("#contentBox");
					
					contentBox.getChildren().add(new BorderPane(pluginContent));
					Separator contentLine = new Separator();
					contentLine.setPrefWidth(215.0);
					contentLine.setPadding(new Insets(5.0, 0.0, 0.0, 0.0));
					contentBox.getChildren().add(contentLine);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void clearPluginGraphic() {
		((VBox) stage.getScene().lookup("#contentBox")).getChildren().clear();
		((VBox) stage.getScene().lookup("#contentLoadingBox")).getChildren().clear();
		
		// it could be null if occur error during initialize
		if (popOver != null) {
			popOver.hide();
		}
	}
	
	boolean isActivated() {
		JFXToggleButton togActivator = (JFXToggleButton) stage.getScene().lookup("#togActivator");
		return togActivator.selectedProperty().get();
	}
	
	void clearPluginGraphicAndDeactivate() throws Exception {
		clearPluginGraphic();
		plugin.pluginDeactivated();
	}
	
	private void activated() {
		displayLoadingBar(true);
		
		new Thread(() -> {
			Platform.runLater(() -> {
				try {
					plugin.pluginActivated();
					loadPluginGraphic();
				} catch (Throwable e) {
					makeDisable(e, true);
				} finally {
					displayLoadingBar(false);
				}
				
				stage.getScene().getWindow().sizeToScene(); // debug mode specific
			});
		}).start();
	}
	
	private void deactivated() {
		new Thread(() -> {
			Platform.runLater(() -> {
				try {
					clearPluginGraphicAndDeactivate();
				} catch (Throwable e) {
					makeDisable(e, true);
				}
				
				stage.getScene().getWindow().sizeToScene(); // debug mode specific
			});
		}).start();
	}
	
	static class RESTfulAPI extends org.silentsoft.net.rest.RESTfulAPI {

		public static <T> T doGet(String uri, Object param, Class<T> returnType) throws Exception {
			return doGet(uri, getProxyHost(), param, returnType, (request) -> {
				request.setHeaders(createHeaders());
			});
		}
		
		public static <T> T doPost(String uri, Object param, Class<T> returnType) throws Exception {
			return doPost(uri, getProxyHost(), param, returnType, (request) -> {
				request.setHeaders(createHeaders());
			});
		}
		
		// TODO : proxy host may needs caching logic.
		public static HttpHost getProxyHost() {
			HttpHost proxyHost = null;
			
			try {
				if (DebugApp.proxyHost == null) {
					List<Proxy> proxies = ProxySearch.getDefaultProxySearch().getProxySelector().select(URI.create("http://actlist.silentsoft.org"));
					if (proxies != null && proxies.isEmpty() == false) {
						for (Proxy proxy : proxies) {
							SocketAddress socketAddress = proxy.address();
							if (socketAddress instanceof InetSocketAddress) {
								InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
								proxyHost = new HttpHost(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
								break;
							}
						}
					}
				} else {
					URI uri = URI.create(DebugApp.proxyHost);
					proxyHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
				}
			} catch (Exception e) {
				
			}
			
			return proxyHost;
		}
		
		private static Header[] createHeaders() {
			ArrayList<Header> headers = new ArrayList<Header>();
			
			StringBuffer userAgent = new StringBuffer();
			userAgent.append("Actlist-0.0.0");
			
			if (SystemUtil.isWindows()) {
				userAgent.append(" windows-");
			} else if (SystemUtil.isMac()) {
				userAgent.append(" macosx-");
			} else if (SystemUtil.isLinux()) {
				userAgent.append(" linux-");
			} else {
				userAgent.append(" unknown-");
			}
			userAgent.append(SystemUtil.getOSArchitecture());
			
			userAgent.append(" platform-");
			userAgent.append(SystemUtil.getPlatformArchitecture());
			
			headers.add(new BasicHeader("user-agent", userAgent.toString()));
			
			return headers.size() == 0 ? null : headers.toArray(new Header[headers.size()]);
		}
		
	}

}