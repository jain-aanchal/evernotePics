package displaynotepics


import com.evernote.edam.notestore.NoteStore
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.THttpClient
import com.evernote.edam.type.Notebook
import org.scribe.builder.ServiceBuilder
import org.scribe.oauth.OAuthService
import org.scribe.model.Token
import org.scribe.model.Verifier
import com.evernote.oauth.consumer.AccessTokenExtractor
import com.evernote.edam.notestore.NotesMetadataResultSpec
import com.evernote.edam.type.NoteSortOrder
import com.evernote.edam.notestore.NoteFilter
import com.evernote.edam.notestore.NotesMetadataList
import com.evernote.edam.notestore.NoteMetadata
import com.evernote.edam.type.Note
import com.evernote.edam.type.Resource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import java.io.InputStream
import javax.servlet.ServletContext

class LoginController {

    private String userAgent = CH.config.evernote.useragent
    private String evernoteHost =   CH.config.evernote.host.url

    private String consumerKey =   CH.config.evernote.consumer.key
    private String consumerSecret =  CH.config.evernote.consumer.secret

    private String cbUrl= CH.config.grails.serverURL+"/login/accessToken"

    static final String KEY_REQUEST_TOKEN = "requestToken"
    static final String KEY_REQUEST_TOKEN_SECRET = "requestTokenSecret"
    static final String KEY_ACCESS_TOKEN = "accessToken"
    static final String NOTESTORE_URL = "noteStoreUrl"
    static final String USER_ID = "UserId"

    OAuthService service = new ServiceBuilder()
            .provider(com.evernote.oauth.consumer.EvernoteApi.EvernoteSandboxApi.class)
            .apiKey(consumerKey)
            .apiSecret(consumerSecret)
            .callback(cbUrl)
            .build();

    def index() { }

    //Check if we have the access token and the notebookUrl
    def detectAuth(){
        String actionFlow = "authenticate"
        if (session.getAttribute(KEY_ACCESS_TOKEN) != null && session.getAttribute(NOTESTORE_URL) != null){
            actionFlow =  "getUrls"
        }
        redirect(action: actionFlow)
    }

    //Authenticates the user
    def authenticate() {

        cleanSession()

        Token requestToken = service.getRequestToken();
        session.setAttribute(KEY_REQUEST_TOKEN, requestToken.getToken());
        session.setAttribute(KEY_REQUEST_TOKEN_SECRET, requestToken.getSecret());

        def authUrl = service.getAuthorizationUrl(requestToken)

        redirect(url: authUrl)
    }
    //Get the Access Token and NoteSoreUrl
    def accessToken() {

        Token requestToken = new Token(session.getAttribute(KEY_REQUEST_TOKEN), session.getAttribute(KEY_REQUEST_TOKEN_SECRET))


        Verifier requestVerifier = new Verifier(params.oauth_verifier);
        AccessTokenExtractor.EvernoteAuthToken token =
            (AccessTokenExtractor.EvernoteAuthToken) service.getAccessToken(requestToken, requestVerifier);
        def accessToken = token.getToken()
        def noteStoreUrl = token.noteStoreUrl
        def userId = token.userId

        session.setAttribute(KEY_ACCESS_TOKEN, accessToken);
        session.setAttribute(NOTESTORE_URL, noteStoreUrl);
        session.setAttribute(USER_ID, userId);

        redirect(action: 'getUrls')

    }

    //get the Image Urls
    def getUrls(){
        List<String> urls = getImageUrl(0, 5)

        redirect(action: "welcome" ,params: [url: urls])
    }

    //View for the welcome page which displays the user images
    def welcome() {

    }

    //Clean the default session first
    private cleanSession() {
        session.removeAttribute(KEY_REQUEST_TOKEN)
        session.removeAttribute(KEY_REQUEST_TOKEN_SECRET)
        session.removeAttribute(KEY_ACCESS_TOKEN)
    }

    //Get the NoteStore Client
    private NoteStore.Client getNoteStoreClient() {

        String noteStoreUrl = session.getAttribute(NOTESTORE_URL);

        THttpClient noteStoreTrans = new THttpClient(noteStoreUrl);
        noteStoreTrans.setCustomHeader("User-Agent", userAgent);
        TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
        NoteStore.Client noteStore = new NoteStore.Client(noteStoreProt, noteStoreProt);
        return noteStore
    }

   //Get Notes Metadata based on the start Index and Offset
    private NotesMetadataList getNote(int startIndex, int pageSize) {
        String authToken = session.getAttribute(KEY_ACCESS_TOKEN);

        NoteFilter filter = new NoteFilter();
        filter.setOrder(NoteSortOrder.UPDATED.getValue());

        NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
        spec.setIncludeTitle(true);

        NoteStore.Client noteStore = getNoteStoreClient()

        NotesMetadataList notes = noteStore.findNotesMetadata(authToken, filter, startIndex, pageSize, spec);
        return notes


    }

    //Get List of images to be displayed
    public List<String> getImageUrl(int startIndex, int offset) {


        List<String> imageUrl = new ArrayList<String>();


        NotesMetadataList notes = getNote(startIndex, offset)


        int maxNotes = notes.getTotalNotes();


        NoteStore.Client noteStore = getNoteStoreClient()

        int i = 0;

        while (notes != null && startIndex <= maxNotes) {


            for (NoteMetadata note : notes.getNotes()) {


                Note n = noteStore.getNote(session.getAttribute(KEY_ACCESS_TOKEN), note.getGuid(), false, true, false, false)

                if (n.resources != null) {


                    List<Resource> resource = n.resources


                    for (Resource res : resource) {

                        String mimeType = res.getMime()
                        if (mimeType.startsWith("image/") && (res.width >= 256 && res.width <= 1024) && (res.height >= 256 && res.height <= 1024)) {

                            String imageType = mimeType.substring(6, mimeType.length())

                            String fileName = grailsApplication.config.tomcat.webapps.path+ "/save" + i + "." + imageType

                            String fileN = "save" + i + "." + imageType
                            i++;

                            File outputfile = new File(fileName)

                            outputfile.bytes =  res.getData().body

                            imageUrl.add(fileN)

                        }  //if
                    } //for
                }//if
            }//for

            startIndex += offset
            notes = getNote(startIndex, offset)
        }

        return imageUrl;

    }


}
