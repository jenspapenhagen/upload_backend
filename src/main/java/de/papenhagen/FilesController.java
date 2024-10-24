package de.papenhagen;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Produces(MediaType.TEXT_HTML)

public class FilesController {

    private static final Logger LOG = Logger.getLogger(FilesController.class);

    private static final List<String> WHITELIST = List.of("pdf", "jpg", "jpeg");

    @ConfigProperty(name = "upload.path", defaultValue = "uploadfiles")
    String uploadPath;

    @Location("files.html")
    Template template;

    @Location("successUpload.html")
    Template successTemplate;

    @Path("files.html")
    @GET
    public TemplateInstance get() {
        return template.instance();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateInstance upload(final MultipartFormDataInput input) throws IOException {

        //Get API input data
        Map<String, Collection<FormValue>> uploadForm = input.getValues();

        //Get file data to save
        for (FormValue inputPart : uploadForm.get("fileupload")) {
            final FileItem fileItem = inputPart.getFileItem();
            LOG.info("filesize: " + fileItem.getFileSize() + " Bytes");

            final String contentType = Files.probeContentType(fileItem.getFile());
            if (!WHITELIST.contains(contentType)) {
                LOG.error("File not allowed");
                return template.instance();
            }

            //WARNING: we are losing the file postfix.
            final String fileName = fileItem.getFile().toFile().getName();

            //move the given file into the uploadFile folder
            final java.nio.file.Path currentWorkingDir = Paths.get("").toAbsolutePath();
            final java.nio.file.Path path = Paths.get(currentWorkingDir.toString(), uploadPath, fileName);

            try {
                fileItem.write(path);
                LOG.debug("success upload");
            } catch (IOException ex) {
                LOG.error("IOException on creating the file: " + ex.getLocalizedMessage());
                return template.instance();
            }

        }

        return successTemplate.instance();
    }

}
