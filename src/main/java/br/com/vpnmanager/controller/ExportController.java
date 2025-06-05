package br.com.vpnmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;

@RestController
@RequestMapping("/exports")
public class ExportController {

    private static final Path EXPORT_DIR = Paths.get("/opt/easy-rsa/exports/");

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadExportedFile(@PathVariable String filename,
            HttpServletRequest request,
            Principal principal) throws IOException {
        // Filename validation
        if (!filename.matches("^[\\w\\-]+\\.zip$")) {
            return ResponseEntity.badRequest().build();
        }

        // Check if the requester is the owner or has admin role
        String expectedUsername = filename.split("_")[0];
        String currentUsername = principal.getName();
        boolean isAdmin = request.isUserInRole("ADMIN");
        if (!expectedUsername.equals(currentUsername) && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path filePath = EXPORT_DIR.resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
