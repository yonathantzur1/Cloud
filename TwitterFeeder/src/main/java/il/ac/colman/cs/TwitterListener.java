package il.ac.colman.cs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import il.ac.colman.cs.util.CloudWatch;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Date;

public class TwitterListener {
    public static void main(String[] args) {
        // Create our twitter configuration
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(System.getProperty("TWITTER_API_KEY"))
                .setOAuthConsumerSecret(System.getProperty("TWITTER_API_SECRET_KEY"))
                .setOAuthAccessToken(System.getProperty("TWITTER_ACCESS_TOKEN"))
                .setOAuthAccessTokenSecret(System.getProperty("TWITTER_ACCESS_TOKEN_SECRET"));

        // Create our Twitter stream
        TwitterStreamFactory tf = new TwitterStreamFactory(cb.build());
        TwitterStream twitterStream = tf.getInstance();

        twitterStream.addListener(new StatusListener() {
            Date tweetDate = null;
            CloudWatch cw = new CloudWatch();
            AmazonSQS client = AmazonSQSAsyncClientBuilder.defaultClient();

            public void onException(Exception e) {

            }

            public void onStatus(Status status) {
                // In case the tweet language is English.
                if (isWaitTimeoutOver(tweetDate) && status.getLang().equals("en")) {
                    tweetDate = new Date();
                    cw.SendMetric("receive_tweet", 1.0);

                    // Getting url links in tweet.
                    URLEntity urls[] = status.getURLEntities();

                    if (urls.length > 0) {
                        URLEntity url = urls[0];
                        ObjectMapper om = new ObjectMapper();
                        JsonNode dataJson = om.createObjectNode();

                        // Build json to send to the queue.
                        ((ObjectNode) dataJson).put("link", url.getExpandedURL());
                        ((ObjectNode) dataJson).put("track", System.getProperty("track"));

                        try {
                            // Send data as a json string to the SQS.
                            client.sendMessage(System.getProperty("queue_url"), dataJson.toString());
                        }
                        // Reconnect to the SQS client again in case of error.
                        catch (Exception e) {
                            System.out.println("onStatus - " + e.getMessage());
                            client = AmazonSQSAsyncClientBuilder.defaultClient();
                        }
                    }
                }
            }

            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

            }

            public void onTrackLimitationNotice(int i) {

            }

            public void onScrubGeo(long l, long l1) {

            }

            public void onStallWarning(StallWarning stallWarning) {

            }

            // Set timer interval to make the tweets stream slower.
            private boolean isWaitTimeoutOver(Date tweetDate) {
                return (tweetDate == null ||
                        tweetDate.getTime() + Integer.parseInt(System.getProperty("tweets_stream_delay")) < new Date().getTime());
            }
        });

        // Stream tweets by asked track value.
        twitterStream.filter(System.getProperty("track"));
    }
}
