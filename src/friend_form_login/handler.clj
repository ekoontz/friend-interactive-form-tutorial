(ns friend-form-login.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [friend-form-login.lib
             :refer [authentications client-config
                     google-auth-config display-authentications
                     token2username users]]
            [clojure.string :as string]
            [friend-oauth2.workflow :as oauth2]
            [clojure.tools.logging :as log]
            [friend-oauth2.util :refer [format-config-uri]]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))
(defroutes app-routes
  (GET "/" request (str "<h1>Hello World</h1>"
                        (if (not (empty? (authentications request)))
                          (display-authentications request))
                        "<p>"
                        (if (not (empty? (authentications request)))
                          (string/join " | "
                                       ["<a href='/authenticated-only'>For authenticated people only</a>"
                                        "<a href='/admin'>For admins only</a>"
                                        "<a href='/logout'>Logout</a>"])
                          " <a href='/login/form'>Login</a> ")))

  (GET "/authenticated-only" request
       (friend/authorize #{::user}
                         (str "<h1>For authenticated users</h1>"
                              (display-authentications request)
                              "<p>This page can only be seen by authenticated users.</p>"
                              "<a href='/'>Home</a>")))

  (GET "/admin" request
       (friend/authorize #{::admin}
                         (str "<h1>For administrators</h1>"
                              (display-authentications request)
                              "<p>This page can only be seen by administrators.</p>"
                              "<a href='/'>Home</a>")))

  ;; Note that in our configuration of friend, we've made the login-url be /login ..
  (GET "/login" [] (-> "login.html" ;; note that this won't be used if the user is not
                       ;; already authenticated, because we are using two workflows:
                       ;; 1. friend-workflows/interactive-form
                       ;; 2. oauth2/workflow
                       ;; Friend will simply do the google workflow.
                       ;; If however the user *is* authenticated, (whether locally or oauth2)
                       ;; then login.html will be used and the form will be shown.
                       (ring.util.response/file-response {:root "resources"})
                       (ring.util.response/content-type "text/html")))

  ;; .. but we also supply this to allow us to access the form login.
  ;; If the user is not currently authenticated, /login will go straight to google,
  ;; skipping our local (login.html) login page. So /login/form avoids this forwarding
  ;; behavior that friend has, allowing us to see the local login page.
  (GET "/login/form" [] (-> "login.html"
                       (ring.util.response/file-response {:root "resources"})
                       (ring.util.response/content-type "text/html")))

  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/login/form")))

  (route/not-found "Not Found"))

(def app
  (handler/site
   (friend/authenticate app-routes
   			{:credential-fn (partial creds/bcrypt-credential-fn users)
                         :workflows [(workflows/interactive-form)
                                     (oauth2/workflow google-auth-config)]})))

