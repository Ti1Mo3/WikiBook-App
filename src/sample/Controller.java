/** @author Teimurazi Euashvili(Matrikelnummer:18808) */
package sample;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Scanner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.control.TextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;
import sample.bibliothek.*;
import sample.exceptions.*;

public class Controller implements Initializable {

  @FXML public WebView webView;
  @FXML public TextField bookTitle;
  @FXML public Label lastEditor;
  @FXML public Label lastEdition;
  @FXML public ListView zListView;
  public ChoiceBox sort;
  @FXML public ListView synonymView;
  @FXML public Button synonymButton;
  @FXML public Button backButton;
  @FXML public Button nextButton;
  @FXML public ChoiceBox termHistory;
  @FXML public ComboBox termChoice;
  /** Variables for navigate between next and previous elements * */
  String lastItem;
  String nextItem;
  int counter = 1;
  int termID = -1;
  /** Save actions, which is needed to call the function * */
  ActionEvent webAction;
  ActionEvent buttonClicked;
  ActionEvent buttonEvent;
  /** Array list for list view, combo box and choice box * */
  ObservableList<String> sortWay = FXCollections.observableArrayList("A-Z", "Z-A");
  ObservableList<Integer> termItems = FXCollections.observableArrayList();
  ObservableList<String> zettelkastenItems = FXCollections.observableArrayList();
  ObservableList<String> synonymItems = FXCollections.observableArrayList();
  ObservableList<String> termComboBox = FXCollections.observableArrayList();

  public WebEngine engine;

  Zettelkasten myZettelkasten = new Zettelkasten();
  ArrayList<String> searchedItems = new ArrayList<String>();

  @Override
  public void initialize(URL _url, ResourceBundle _resourceBundle) {
    sort.setItems(sortWay);
    sort.setValue("A-Z");
    termHistory.setValue(termID);
    engine = webView.getEngine();
    nextButton.setDisable(true);
  }
  /** Shows the book with the given title in the wikibook.org * */
  public void showWebsite(ActionEvent _actionEvent)
      throws IOException, SAXException, BookNotFoundException, ParseException, SynonymNotFound {
    /** Get the action * */
    webAction = _actionEvent;
    /** Initialize Wikibook * */
    Wikibook wikiBookMedium = null;
    /** Active the synonym area * */
    synonymView.setDisable(false);
    synonymButton.setDisable(false);
    /** parse title and load the link in the internet * */
    String title = bookTitle.getText().replace(" ", "_");
    /** crops the Array */
    if (!(searchedItems.contains(title))
        && !(nextItem == null)
        && !(searchedItems.indexOf(lastItem) == searchedItems.size() - 1)) {
      int lastItemIndex = searchedItems.indexOf(lastItem); // 2
      System.out.println(lastItemIndex);
      searchedItems.removeIf(i -> ((searchedItems.indexOf(i) > lastItemIndex)));
      termComboBox.removeIf(item -> (termComboBox.indexOf(item) < termComboBox.indexOf(lastItem)));
      termItems.removeIf(id -> (termItems.indexOf(id) > lastItemIndex));
      termID = lastItemIndex;
      counter = 1;
      backButton.setDisable(false);
      nextButton.setDisable(true);
    }
    /** Adds the item to the Searched history list, if the item doesnt included * */
    if (!(searchedItems.contains(title))) {
      searchedItems.add(title);
      /** Add the item Combobox too * */
      if (termComboBox.size() == 0) {
        termComboBox.add(bookTitle.getText());
      } else {
        /** Add new Element as the first element of the array (in reverse chronological order) * */
        termComboBox.add(0, bookTitle.getText());
      }
      /** configure the history of terms(ID) * */
      termID++;
      termItems.add(termID);
      termHistory.setItems(termItems);
      termHistory.setValue(termItems.get(termItems.size() - 1));
      /** configure the history of terms with(Title) */
      termChoice.setItems(termComboBox);
      termChoice.setValue(termComboBox.get(0));
    }
    /** Create a link to load the website * */
    String link = "http://de.wikibooks.org/wiki/" + bookTitle.getText();
    engine.load(link);
    /** Search Synonyms for the given title * */
    searchSynonym(buttonClicked);
    /** Give the error message, if the book not found in Wikibook * */
    try {
      wikiBookMedium = (Wikibook) myZettelkasten.findWikiBook(title);
      if (wikiBookMedium == null) {
        throw new BookNotFoundException(
            "Das Buch mit dem Titel " + "'" + bookTitle.getText() + "'" + " ist nicht gefunden");
      }
    } catch (IOException | BookNotFoundException _e) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setHeaderText("BookNotFound Error");
      alert.setContentText(_e.getMessage());
      alert.showAndWait();
    }
    /** prints the last editor and the time of the last edit * */
    lastEditor.setText("Letzter Bearbeiter " + wikiBookMedium.getAutor());
    lastEdition.setText("Letzte Änderung " + wikiBookMedium.getLetzteBearbeitung());
  }
  /** Calls the function showWebsite if enter pressed * */
  public void enterPressed(KeyEvent _keyEvent)
      throws IOException, SAXException, BookNotFoundException, ParseException, SynonymNotFound {
    if (_keyEvent.getCode() == KeyCode.ENTER) {
      showWebsite(buttonEvent);
    }
  }

  /**
   * Adds the WikiBook to the Zettelkasten and displays it on the Medium LisView
   *
   * @param _actionEvent
   * @throws IOException
   * @throws SAXException
   * @throws BookNotFoundException
   */
  public void addMedium(ActionEvent _actionEvent)
      throws IOException, SAXException, BookNotFoundException {
    Wikibook wikiBookMedium;
    String title = bookTitle.getText().replace(" ", "_");
    wikiBookMedium = (Wikibook) myZettelkasten.findWikiBook(title);
    myZettelkasten.addMedium(wikiBookMedium);
    zettelkastenItems.add(
        myZettelkasten
            .getMyZettelkasten()
            .get(myZettelkasten.getMyZettelkasten().size() - 1)
            .getTitel());
    zListView.setItems(zettelkastenItems);
  }

  /**
   * Sorts the Zettelkasten Array and displays it sorted on the Medium LisView
   *
   * @param _actionEvent
   */
  public void sortArray(ActionEvent _actionEvent) {
    String sortAlgorithm = (String) sort.getValue();
    zettelkastenItems.clear();
    myZettelkasten.sort(sortAlgorithm);
    for (int i = 0; i < myZettelkasten.getMyZettelkasten().size(); i++) {
      zettelkastenItems.add(myZettelkasten.getMyZettelkasten().get(i).getTitel());
    }
    zListView.setItems(zettelkastenItems);
  }

  /**
   * Serialises the object of Zettelkasten and saves it in the file 'myZettelkasten'
   *
   * @param _actionEvent
   * @throws IOException
   */
  public void save(ActionEvent _actionEvent) throws IOException {
    BinaryPersisenty dataSaver = new BinaryPersisenty();
    dataSaver.save(myZettelkasten, "myZettelkasten");
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setHeaderText("Gespeichert");
    alert.setContentText("Zettelkasten ist mit der Name 'myZettelkasten' erfolgreich gespeichert");
    alert.showAndWait();
  }

  /**
   * Deserializes the Object from the file: 'myZettelkasten' and displays it on the Medium ListView
   *
   * @param _actionEvent
   * @throws FileNotFoundException
   */
  public void load(ActionEvent _actionEvent) throws FileNotFoundException {
    BinaryPersisenty dataLoader = new BinaryPersisenty();
    myZettelkasten = dataLoader.load("myZettelkasten");
    zettelkastenItems.clear();
    for (int i = 0; i < myZettelkasten.getMyZettelkasten().size(); i++) {
      zettelkastenItems.add(myZettelkasten.getMyZettelkasten().get(i).getTitel());
    }
    zListView.setItems(zettelkastenItems);
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setHeaderText("Loaded");
    alert.setContentText("Die zuletzt gespeicherte Zettelkasten ist erfolgreich geladen");
    alert.showAndWait();
  }

  /**
   * Searches the synonymous with the given title and displays the result on the Synonym ListView
   *
   * @throws IOException
   * @throws ParseException
   */
  public void searchSynonym(ActionEvent _actionEvent)
      throws IOException, ParseException, SynonymNotFound, BookNotFoundException, SAXException {
    String toFind;
    String selectedItem = (String) synonymView.getSelectionModel().getSelectedItem();
    _actionEvent = buttonClicked;
    if (selectedItem == null) {
      toFind = bookTitle.getText();
      synonymItems.clear();
    } else {
      toFind = selectedItem;
      bookTitle.setText(toFind);
      synonymItems.clear();
      synonymView.setItems(synonymItems);
      showWebsite(webAction);
    }
    String BasisUrl = "http://api.corpora.uni-leipzig.de/ws/similarity/";
    String Corpus = "deu_news_2012_1M";
    String Anfragetyp = "/coocsim/";
    String Parameter = "?minSim=0.1&limit=50";
    String Suchbegriff = toFind;
    URL myURL;
    myURL = new URL(BasisUrl + Corpus + Anfragetyp + Suchbegriff + Parameter);
    JSONParser jsonParser = new JSONParser();
    try {
      String jsonResponse = streamToString(myURL.openStream());
      // den gelieferten String in ein Array parsen
      JSONArray jsonResponseArr = (JSONArray) jsonParser.parse(jsonResponse);
      try {
        if (jsonResponseArr.size() < 1) {
          synonymItems.clear();
          throw new SynonymNotFound();
        }
        // das erste Element in diesem Array
        JSONObject firstEntry = (JSONObject) jsonResponseArr.get(0);
        // aus dem Element den Container für das Synonym beschaffen
        JSONObject wordContainer = (JSONObject) firstEntry.get("word");
        // aus dem Container das Synonym beschaffen
        String synonym = (String) wordContainer.get("word");
        // alle abgefragten Synonyme
        for (Object el : jsonResponseArr) {
          wordContainer = (JSONObject) ((JSONObject) el).get("word");
          synonym = (String) wordContainer.get("word");
          synonymItems.add(synonym);
        }
        /** Sort the Array Ascending * */
        Collections.sort(synonymItems);
        /** Display Synonyms on the Synonym view * */
        synonymView.setItems(synonymItems);
        /** If Synonym not found hide the synonym view and the button */
      } catch (SynonymNotFound err) {
        synonymItems.add("<keine>");
        synonymView.setItems(synonymItems);
        synonymView.setDisable(true);
        synonymButton.setDisable(true);
      }
      /** Catch the Network error * */
    } catch (Exception err) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setHeaderText("ERROR");
      alert.setContentText("Fehler beim Zugriff auf den Wortschatzes");
      alert.showAndWait();
    }
  }
  /**
   * Reads InputStream's contents into String
   *
   * @param is - the InputStream
   * @return String representing is' contents
   * @throws IOException
   */
  public static String streamToString(InputStream is) throws IOException {
    String result = "";
    // other options:
    // https://stackoverflow.com/questions/309424/read-convertan-inputstream-to-a-string
    try (Scanner s = new Scanner(is)) {
      s.useDelimiter("\\A");
      result = s.hasNext() ? s.next() : "";
      is.close();
    }
    return result;
  }
  /** calls the function searchsynonym, if the item in the Listview is selected * */
  public void mouseClicked(MouseEvent _mouseEvent)
      throws SynonymNotFound, BookNotFoundException, SAXException, ParseException, IOException {
    if (_mouseEvent.getClickCount() == 2) {
      searchSynonym(buttonClicked);
    }
  }
  /** Shows the previous Item * */
  public void previousItem(ActionEvent _actionEvent)
      throws SynonymNotFound, SAXException, BookNotFoundException, ParseException, IOException {
    counter++;
    nextButton.setDisable(false);
    lastItem = (searchedItems.get(searchedItems.size() - counter));
    nextItem = searchedItems.get(searchedItems.indexOf(lastItem) + 1);
    bookTitle.setText(lastItem.replace("_", " "));
    if (searchedItems.indexOf(lastItem) == 0) {
      backButton.setDisable(true);
    }
    termHistory.setValue(termItems.get(termItems.indexOf(termHistory.getValue()) - 1));
    termChoice.setValue(termComboBox.get(termComboBox.indexOf(termChoice.getValue()) + 1));
    showWebsite(webAction);
  }
  /** Shows the next Item * */
  public void nextItem(ActionEvent _actionEvent)
      throws SynonymNotFound, SAXException, BookNotFoundException, ParseException, IOException {
    nextItem = searchedItems.get(searchedItems.indexOf(lastItem) + 1);
    if (searchedItems.indexOf(nextItem) == (searchedItems.size() - 1)) {
      nextButton.setDisable(true);
    }
    bookTitle.setText(nextItem);
    backButton.setDisable(false);
    counter--;
    lastItem = (searchedItems.get(searchedItems.size() - counter));
    termHistory.setValue(termItems.get(termItems.indexOf(termHistory.getValue()) + 1));
    termChoice.setValue(termComboBox.get(termComboBox.indexOf(termChoice.getValue()) - 1));
    showWebsite(webAction);
  }
 /** prints the item, which is selected in the combobox **/
  public void anotherItemSelected(ActionEvent _actionEvent)
      throws SynonymNotFound, SAXException, BookNotFoundException, ParseException, IOException {
    /** Check whether it is another item or not * */
    if (!(bookTitle.getText().equals(termChoice.getValue()))) {
      bookTitle.setText((String) termChoice.getValue());
      showWebsite(webAction);
    }
  }
/** Gives the Information about the Application **/
  public void giveInfo(ActionEvent _actionEvent) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.setHeaderText("Information");
    alert.setContentText(
        "Alle redaktionellen Inhalte stammen von den Internetseiten der Projekte Wikibooks und Wortschatz.\n"
            + "Die von Wikibooks bezogenen Inhalte unterliegen seit dem 22. Juni 2009 unter der Lizenz CC-BY-SA 3.0\n"
            + "Unported zur Verfügung. Eine deutschsprachige Dokumentation für Weiternutzer findet man in den\n"
            + "Nutzungsbedingungen der Wikimedia Foundation. Für alle Inhalte von Wikibooks galt bis zum 22. Juni\n"
            + "2009 standardmäßig die GNU FDL (GNU Free Documentation License, engl. für GNU-Lizenz für freie\n"
            + "Dokumentation). Der Text der GNU FDL ist unter\n"
            + "http://de.wikipedia.org/wiki/Wikipedia:GNU_Free_Documentation_License verfügbar.\n"
            + "Die von Wortschatz (http://wortschatz.uni-leipzig.de/) bezogenen Inhalte sind urheberrechtlich geschützt.\n"
            + "Sie werden hier für wissenschaftliche Zwecke eingesetzt und dürfen darüber hinaus in keiner Weise\n"
            + "genutzt werden.\n"
            + "Dieses Programm ist nur zur Nutzung durch den Programmierer selbst gedacht. Dieses Programm dient\n"
            + "der Demonstration und dem Erlernen von Prinzipien der Programmierung mit Java. Eine Verwendung\n"
            + "des Programms für andere Zwecke verletzt möglicherweise die Urheberrechte der Originalautoren der\n"
            + "redaktionellen Inhalte und ist daher untersagt");
    alert.showAndWait();
  }
}
