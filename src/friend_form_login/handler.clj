(ns friend-form-login.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :refer [format-config-uri]]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))

(def users {"admin" {:username "admin"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::admin}}
            "dave" {:username "dave"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::user}}})

(def client-config
  {:client-id (env :google-client-id)
   :client-secret (env :google-client-secret)
   :callback {:domain (env :google-callback-domain)
              :path "/oauth2callback"}})

(derive ::admin ::user)

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/authorized" request
       (friend/authorize #{::user} "This page can only be seen by authenticated users."))
  (GET "/admin" request
       (friend/authorize #{::admin} "This page can only be seen by administrators."))
  (GET "/login" [] (-> "login.html"
                       (ring.util.response/file-response {:root "resources"})
                       (ring.util.response/content-type "text/html")))
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/not-found "Not Found"))

(defn token2username [access-token & [request]]
  "Get user's email address from the vc_user table, given their access token. 
   If access-token is not registered in database,
   then ask Google for the user's email address and add as new row to the vc_user table.
   _request_ is optional: if present, it allows for more diagnostic logging than without it.
"
  (cond
   (nil? access-token)
   (do
     (throw (Exception. (str "token2user: supplied access-token was null."))))

   true
   (do
     (let [{:keys [status headers body error] :as resp} 
           @(http/get 
             (str "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" access-token))]
       (cond 
                 
             (not (= status 200))
             (do
               nil)
                 
             true
             (do
               (let [body (json/read-str body
                                         :key-fn keyword
                                         :value-fn (fn [k v]
                                                     v))
                     email (get body :email)
                     given-name (get body :given_name)
                     family-name (get body :family_name)
                     picture (get body :picture)]
                 email)))))))

(defn credential-fn [token]
  ;;lookup token in DB or whatever to fetch appropriate :roles
;  (log/info (str "google/credential-fn token: " (:access-token token)))
  (let [username (token2username (:access-token token))]
    {:identity token :roles #{::user}}))
  
(def uri-config
  {:authentication-uri {:url "https://accounts.google.com/o/oauth2/auth"
                        :query {:client_id (:client-id client-config)
                                :response_type "code"
                                :redirect_uri (format-config-uri client-config)
                                :scope "email"}}

   :access-token-uri {:url "https://accounts.google.com/o/oauth2/token"
                      :query {:client_id (:client-id client-config)
                              :client_secret (:client-secret client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (format-config-uri client-config)}}})

(def google-auth-config {:client-config client-config
                         :uri-config uri-config
                         :credential-fn credential-fn})

(def app
  (handler/site
   (friend/authenticate app-routes
   			{:credential-fn (partial creds/bcrypt-credential-fn users)
                         :workflows [(workflows/interactive-form)
                                     (oauth2/workflow google-auth-config)
                                     ]})))

