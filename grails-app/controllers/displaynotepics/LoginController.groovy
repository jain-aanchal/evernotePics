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



    def authenticate() {
        //if (session.getAttribute(KEY_ACCESS_TOKEN != null && session.getAttribute(NOTESTORE_URL) != null){

        //}

        cleanSession()


        Token requestToken = service.getRequestToken();
        session.setAttribute(KEY_REQUEST_TOKEN, requestToken.getToken());
        session.setAttribute(KEY_REQUEST_TOKEN_SECRET, requestToken.getSecret());

        println("AJ Get Request Session " + session.getAttribute(KEY_REQUEST_TOKEN))

        def authUrl = service.getAuthorizationUrl(requestToken)

        redirect(url: authUrl)
    }

    def accessToken() {
        println("In Accesss Token ")
        Token requestToken = new Token(session.getAttribute(KEY_REQUEST_TOKEN), session.getAttribute(KEY_REQUEST_TOKEN_SECRET))
        println("RequestToken " + requestToken.getToken())

        Verifier requestVerifier = new Verifier(params.oauth_verifier);
        AccessTokenExtractor.EvernoteAuthToken token =
            (AccessTokenExtractor.EvernoteAuthToken) service.getAccessToken(requestToken, requestVerifier);
        def accessToken = token.getToken()
        def noteStoreUrl = token.noteStoreUrl
        def userId = token.userId

        session.setAttribute(KEY_ACCESS_TOKEN, accessToken);

        session.setAttribute(NOTESTORE_URL, noteStoreUrl);
        session.setAttribute(USER_ID, userId);

        println("AJ Verifier: " + requestVerifier.getValue())
        println("AJ AccessToken: " + accessToken)
        println("AJ UserId: " + userId)
        println("AJ....noteStoreUrl " + noteStoreUrl)

        List<String> urls = getImageUrl(0, 5)


        redirect(action: "welcome" ,params: [url: urls])

    }



    def welcome() {

    }

    private cleanSession() {
        session.removeAttribute(KEY_REQUEST_TOKEN)
        session.removeAttribute(KEY_REQUEST_TOKEN_SECRET)
        session.removeAttribute(KEY_ACCESS_TOKEN)
    }

    private NoteStore.Client getNoteStoreClient() {

        String noteStoreUrl = session.getAttribute(NOTESTORE_URL);

        THttpClient noteStoreTrans = new THttpClient(noteStoreUrl);
        noteStoreTrans.setCustomHeader("User-Agent", userAgent);
        TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
        NoteStore.Client noteStore = new NoteStore.Client(noteStoreProt, noteStoreProt);
        return noteStore
    }

    private List<Notebook> getNoteBooks() {
        NoteStore.Client noteStore = getNoteStoreClient()
        String authToken = session.getAttribute(KEY_ACCESS_TOKEN);

        List<Notebook> notebooks = noteStore.listNotebooks(authToken);
        return notebooks

    }

    private NotesMetadataList getNote(int startIndex, int pageSize) {
        String authToken = session.getAttribute(KEY_ACCESS_TOKEN);
        //int pageSize = 10 ;
        println("GetNote Size " + startIndex)
        println("get Note pageSize " + pageSize)
        NoteFilter filter = new NoteFilter();
        filter.setOrder(NoteSortOrder.UPDATED.getValue());

        NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
        spec.setIncludeTitle(true);

        NoteStore.Client noteStore = getNoteStoreClient()

        NotesMetadataList notes = noteStore.findNotesMetadata(authToken, filter, startIndex, pageSize, spec);
        return notes


    }

    public List<String> getImageUrl(int startIndex, int offset) {


        List<String> imageUrl = new ArrayList<String>();


        NotesMetadataList notes = getNote(startIndex, offset)


        int maxNotes = notes.getTotalNotes();

        println("Max notes " + maxNotes)
        // println("Notes THIS page " + notesThisPage)

        NoteStore.Client noteStore = getNoteStoreClient()

        int i = 0;

        while (notes != null && startIndex <= maxNotes) {

            int notesThisPage = notes.getNotes().size();
            println("AJ...notes this page " + notesThisPage)

            for (NoteMetadata note : notes.getNotes()) {


                Note n = noteStore.getNote(session.getAttribute(KEY_ACCESS_TOKEN), note.getGuid(), false, true, false, false)

                if (n.resources != null) {
                    println("AJ....n " + n.resources.toString())
                    println("Finding the resource ")

                    List<Resource> resource = n.resources


                    for (Resource res : resource) {

                        String mimeType = res.getMime()
                        if (mimeType.startsWith("image/") && (res.width >= 256 && res.width <= 1024) && (res.height >= 256 && res.height <= 1024)) {

                            println("AJ.. image found ")

                            String imageType = mimeType.substring(6, mimeType.length())

                            println("image type " + imageType)

                            InputStream ins = new ByteArrayInputStream(res.getData().body)

                            BufferedImage bImageFromConvert = ImageIO.read(ins)
                            String fileName = "web-app/images/save" + i + "." + imageType

                            println("Filename " + fileName)
                            String fileN = "/images/save" + i + "." + imageType
                            i++;

                            File outputfile = new File(fileName)

                            println("AJ...outputFile " + fileN)
                            try {
                                println("ImageType " + imageType)

                                ImageIO.write(bImageFromConvert, imageType, outputfile)
                            }
                            catch (Exception e) {
                                e.printStackTrace()
                            }
                            String image =   fileN
                            imageUrl.add(image)

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
