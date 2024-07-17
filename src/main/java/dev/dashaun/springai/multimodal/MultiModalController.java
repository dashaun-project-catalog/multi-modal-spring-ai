package dev.dashaun.springai.multimodal;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


@RestController
class MultiModalController {

    private final ChatClient chatClient;

    private final Resource kcdc = new ClassPathResource("kcdc.jpg");
    private final Resource tts = new ClassPathResource("tts.mp3");

    public MultiModalController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/kcdcImage")
    String simpleChat(@RequestParam(defaultValue = "How many people are in this picture?") String question) throws Exception {
        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(question)
                        .media(MimeTypeUtils.IMAGE_JPEG, kcdc)
                )
                .call()
                .content();
    }
    
    @GetMapping("/ttsExample")
    public ResponseEntity<byte[]> ttsExample(@RequestParam(defaultValue = "Spring Office Hours is my favorite show!") String text) throws IOException {

        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .withModel("tts-1")
                .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .withResponseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .withSpeed(1.0f)
                .build();

        var openAiAudioApi = new OpenAiAudioApi(System.getenv("OPENAI_API_KEY"));
        var openAiAudioSpeechModel = new OpenAiAudioSpeechModel(openAiAudioApi);
        
        SpeechPrompt speechPrompt = new SpeechPrompt(text, speechOptions);
        SpeechResponse response = openAiAudioSpeechModel.call(speechPrompt);
        
        byte[] responseAsBytes = response.getResult().getOutput();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentLength(responseAsBytes.length);
        headers.set("Content-Disposition", "inline; filename=tts.mp3");

        return new ResponseEntity<>(responseAsBytes, headers, HttpStatus.OK);
    }
    
    @GetMapping("/transcribe")
    String transcribe(){
        var openAiAudioApi = new OpenAiAudioApi(System.getenv("OPENAI_API_KEY"));
        var openAiAudioTranscriptionModel = new OpenAiAudioTranscriptionModel(openAiAudioApi);
        OpenAiAudioApi.TranscriptResponseFormat responseFormat = OpenAiAudioApi.TranscriptResponseFormat.VTT;
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .withLanguage("en")
                .withPrompt("Ask not this, but ask that")
                .withTemperature(0f)
                .withResponseFormat(responseFormat)
                .build();
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(tts, transcriptionOptions);
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
        return response.getResult().getOutput();
    }
    
    
}
