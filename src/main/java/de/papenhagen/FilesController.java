package de.papenhagen;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
@Path("files.html")
public class FilesController {

    private static final Logger LOG = Logger.getLogger(FilesController.class);

    private static final List<String> WHITELIST = List.of("pdf", "jpg", "jpeg");

    @ConfigProperty(name = "upload.path", defaultValue = "uploadfiles")
    String uploadPath;

    @Location("files.html")
    Template template;

    @Location("successUpload.html")
    Template successTemplate;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateInstance upload(final MultipartFormDataInput input) throws IOException {

        //Get API input data
        Map<String, Collection<FormValue>> uploadForm = input.getValues();

        //Get file data to save
        for (FormValue inputPart : uploadForm.get("fileupload")) {
            final FileItem fileItem = inputPart.getFileItem();
            LOG.info("filesize: " + fileItem.getFileSize() + " Bytes");

            final MultivaluedMap<String, String> headers = inputPart.getHeaders();
            final String fileName = getFileName(headers);
            LOG.info("fileName: " + fileName);

            final String fileExtension = getFileExtension(fileName);
            LOG.info("fileNameParts[1]: " + fileExtension);

            if (!WHITELIST.contains(fileExtension)) {
                LOG.error("File not allowed");
                return template.instance();
            }

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

    @GET
    public TemplateInstance get() {
        return template.instance();
    }


    /**
     * This method gives back the fileName.
     *
     * header sample
     * {
     * 	Content-Type=[image/png],
     * 	Content-Disposition=[form-data; name="file"; filename="filename.extension"]
     * }
     **/
    private String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");

                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex >= 0) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

}
