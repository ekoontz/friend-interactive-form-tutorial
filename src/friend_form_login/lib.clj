(ns friend-form-login.lib
  (:require [cemerick.friend :as friend]
            [cemerick.friend
             [credentials :as creds]]
            [environ.core :refer [env]]
            [friend-oauth2.util :refer [format-config-uri]]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(def users {"admin" {:username "admin"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::admin}}
            "dave" {:username "dave"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::user}}})

(def users {"admin" {:username "admin"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::admin}}
            "dave" {:username "dave"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::user}}})

(derive ::admin ::user)

(defn authentications [request]
  "get map of client's authentications and return all of the values."
  (vals (get-in request [:session :cemerick.friend/identity :authentications])))

(defn display-authentications [request]
  (let [auths (authentications request)]
    (str "<h3>Your authentications:</h3><ul><li>" (string/join "</li><li>" auths) "</li></ul>")))

(defn token2username [access-token]
  "Get user's email address given their access token."
  (cond
   (nil? access-token)
   (throw (Exception. (str "token2user: supplied access-token was null.")))

   true
   (do
     (log/info (str "looking up username for google access-token: " access-token))
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

(def client-config
  {:client-id (env :google-client-id)
   :client-secret (env :google-client-secret)
   :callback {:domain (env :google-callback-domain)
              :path "/oauth2callback"}})

(def google-auth-config {:client-config client-config
                         :uri-config {:authentication-uri {:url "https://accounts.google.com/o/oauth2/auth"
                                                           :query {:client_id (:client-id client-config)
                                                                   :response_type "code"
                                                                   :redirect_uri (format-config-uri client-config)
                                                                   :scope "email"}}
                                      :access-token-uri {:url "https://accounts.google.com/o/oauth2/token"
                                                         :query {:client_id (:client-id client-config)
                                                                 :client_secret (:client-secret client-config)
                                                                 :grant_type "authorization_code"
                                                                 :redirect_uri (format-config-uri client-config)}}}
                         :credential-fn (fn [token]
                                          (let [username (token2username (:access-token token))]
                                            {:identity username :roles #{::user}}))})

(defn token2username [access-token]
  "Get user's email address given their access token."
  (cond
   (nil? access-token)
   (throw (Exception. (str "token2user: supplied access-token was null.")))

   true
   (do
     (log/info (str "looking up username for google access-token: " access-token))
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

(def client-config
  {:client-id (env :google-client-id)
   :client-secret (env :google-client-secret)
   :callback {:domain (env :google-callback-domain)
              :path "/oauth2callback"}})

(def google-auth-config {:client-config client-config
                         :uri-config {:authentication-uri {:url "https://accounts.google.com/o/oauth2/auth"
                                                           :query {:client_id (:client-id client-config)
                                                                   :response_type "code"
                                                                   :redirect_uri (format-config-uri client-config)
                                                                   :scope "email"}}
                                      :access-token-uri {:url "https://accounts.google.com/o/oauth2/token"
                                                         :query {:client_id (:client-id client-config)
                                                                 :client_secret (:client-secret client-config)
                                                                 :grant_type "authorization_code"
                                                                 :redirect_uri (format-config-uri client-config)}}}
                         :credential-fn (fn [token]
                                          (let [username (token2username (:access-token token))]
                                            {:identity username :roles #{::user}}))})
