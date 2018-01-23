package com.avairebot.commands.search;

import com.avairebot.AvaIre;
import com.avairebot.chat.MessageType;
import com.avairebot.chat.PlaceholderMessage;
import com.avairebot.contracts.commands.ThreadCommand;
import com.avairebot.factories.MessageFactory;
import net.dv8tion.jda.core.entities.Message;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

public class DuckDuckGoCommand extends ThreadCommand {

    private static final Map<String, String> HTTP_HEADERS = new HashMap<>();

    static {
        HTTP_HEADERS.put("Accept-Language", "en-US,en;q=0.8,en-GB;q=0.6,da;q=0.4");
        HTTP_HEADERS.put("Cache-Control", "no-cache, no-store, must-revalidate");
        HTTP_HEADERS.put("Pragma", "no-cache");
        HTTP_HEADERS.put("Expires", "0");
    }

    public DuckDuckGoCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "DuckDuckGo Command";
    }

    @Override
    public String getDescription() {
        return "Search [DuckDuckGo.com](https://duckduckgo.com/) for whatever you'd like.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <query>` - Searchs DuckDuckGo for your query.");
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command AvaIre Bot`");
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("duckduckgo", "ddg", "g");
    }

    @Override
    public List<String> getMiddleware() {
        return Collections.singletonList("throttle:user,2,5");
    }

    @Override
    public boolean onCommand(Message message, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(message, "Missing argument `query`, you must include your search query.");
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.putAll(HTTP_HEADERS);
            headers.put("User-Agent", "AvaIre-Discord-Bot (" + avaire.getSelfUser().getId() + ")");

            message.getChannel().sendTyping().queue();

            boolean nsfwEnabled = isNSFWEnabled(message);
            Document document = Jsoup.connect(generateUri(args, nsfwEnabled))
                .headers(headers)
                .timeout(10000)
                .get();

            int results = 0;
            List<String> result = new ArrayList<>();
            Elements elements = document.select("div#links div.result");
            for (Element element : elements) {
                Elements link = element.select("h2.result__title a");
                if (isAdvertisement(link)) {
                    continue;
                }

                if (results == 0) {
                    result.add(prepareLinkElement(link) + "\n**See also**");
                    results++;
                    continue;
                }

                result.add(prepareLinkElement(link));
                results++;

                if (results > 5) {
                    break;
                }
            }

            PlaceholderMessage resultMessage = MessageFactory.makeEmbeddedMessage(message.getChannel(), Color.decode("#DE5833"))
                .setDescription(String.join("\n", result))
                .setTitle("Search result for: " + String.join(" ", args))
                .setFooter("NSFW Search is " + (nsfwEnabled ? "Enabled" : "Disabled"));

            if (result.isEmpty() || (result.size() == 1 && result.get(0).startsWith("-1&uddg"))) {
                resultMessage
                    .setColor(MessageType.WARNING.getColor())
                    .setDescription("I found nothing for `:query`")
                    .set("query", String.join(" ", args));
            }

            resultMessage.queue();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            AvaIre.getLogger().error("Failed to complete search query: ", e);
        }
        return false;
    }

    private boolean isAdvertisement(Elements link) {
        return link.attr("href").contains("duckduckgo.com/y.js") ||
            link.attr("href").contains("duckduckgo.com%2Fy.js") ||
            link.attr("rel").startsWith("noopener");
    }

    private String prepareLinkElement(Elements link) throws UnsupportedEncodingException {
        String[] parts = link.attr("href").split("=");

        return URLDecoder.decode(parts[parts.length - 1], "UTF-8");
    }

    private String generateUri(String[] args, boolean isNSFWEnabled) throws UnsupportedEncodingException {
        String url = "https://duckduckgo.com/html/?q=" + URLEncoder.encode(
            removeBangs(String.join(" ", args)), "UTF-8"
        );

        if (isNSFWEnabled) {
            url += "&t=hf&ia=web&kp=-2";
        }
        return url;
    }

    private String removeBangs(String text) {
        if (!Objects.equals(text.substring(0, 1), "!")) {
            return text;
        }
        return removeBangs(text.substring(1, text.length()));
    }

    private boolean isNSFWEnabled(Message message) {
        return !message.getChannelType().isGuild() || message.getTextChannel().isNSFW();
    }
}