(ns kifshare.test.ranges
  (:use [clojure.test])
  (:require [kifshare.ranges :as ranges]))

(deftest test-content-disposition
  (testing "RFC 6266 Content-Disposition header values"
    (are [filename attachment expected]
         (= expected (ranges/content-disposition filename :attachment attachment))

      ;; plain ASCII is unchanged in both the fallback and the extended value
      "report.txt" true  "attachment; filename=\"report.txt\"; filename*=UTF-8''report.txt"
      "report.txt" false "filename=\"report.txt\"; filename*=UTF-8''report.txt"

      ;; Latin-1 umlauts: fallback strips them, filename* carries UTF-8 percent-encoding
      "Müller.txt" true  "attachment; filename=\"M_ller.txt\"; filename*=UTF-8''M%C3%BCller.txt"

      ;; characters beyond Latin-1 (Polish): these are the ones Jetty would have dropped to spaces
      "Łódź.txt" true  "attachment; filename=\"__d_.txt\"; filename*=UTF-8''%C5%81%C3%B3d%C5%BA.txt"

      ;; spaces are kept in the fallback but percent-encoded in filename*
      "my file.txt" true "attachment; filename=\"my file.txt\"; filename*=UTF-8''my%20file.txt"

      ;; quotes and backslashes must not break the quoted fallback string
      "a\"b\\c.txt" true "attachment; filename=\"a_b_c.txt\"; filename*=UTF-8''a%22b%5Cc.txt")))

(deftest test-content-disposition-default-attachment
  (testing "attachment defaults to false"
    (is (= "filename=\"report.txt\"; filename*=UTF-8''report.txt"
           (ranges/content-disposition "report.txt")))))

(deftest test-head-resp-uses-rfc6266
  (testing "head-resp emits an RFC 6266 Content-Disposition without the attachment disposition"
    (let [resp (ranges/head-resp "Müller.txt" 123)]
      (is (= "filename=\"M_ller.txt\"; filename*=UTF-8''M%C3%BCller.txt"
             (get-in resp [:headers "Content-Disposition"])))
      (is (= "123" (get-in resp [:headers "Content-Length"]))))))

(deftest test-non-range-resp-uses-rfc6266
  (testing "non-range-resp emits an RFC 6266 Content-Disposition with the attachment disposition"
    (let [resp (ranges/non-range-resp "body" "Müller.txt" "/zone/home/user/Müller.txt" 456 789 :attachment true)]
      (is (= "attachment; filename=\"M_ller.txt\"; filename*=UTF-8''M%C3%BCller.txt"
             (get-in resp [:headers "Content-Disposition"]))))))

(deftest test-url-encode-path
  (testing "percent-encodes path segments as UTF-8 while preserving slashes"
    (are [path expected] (= expected (ranges/url-encode-path path))

      ;; plain ASCII paths are unchanged
      "/zone/home/file.txt" "/zone/home/file.txt"

      ;; Latin-1 and beyond-Latin-1 segments are UTF-8 percent-encoded
      "/zone/home/user/Müller.txt" "/zone/home/user/M%C3%BCller.txt"
      "/data/Łódź.txt"             "/data/%C5%81%C3%B3d%C5%BA.txt"

      ;; spaces become %20 (not '+'), and URL-significant characters are escaped
      "/a b/c.txt"   "/a%20b/c.txt"
      "/a#b/c?d.txt" "/a%23b/c%3Fd.txt"

      ;; leading and trailing slashes are preserved
      "/foo/bar/" "/foo/bar/"

      ;; the relative redirect target shape is encoded without mangling '..' or separators
      "../d/abc123/Müller.txt" "../d/abc123/M%C3%BCller.txt")))

(deftest test-non-range-resp-encodes-content-location
  (testing "non-range-resp emits a URL-encoded Content-Location"
    (let [resp (ranges/non-range-resp "body" "Müller.txt" "/zone/home/user/Müller.txt" 456 789 :attachment true)]
      (is (= "/zone/home/user/M%C3%BCller.txt"
             (get-in resp [:headers "Content-Location"]))))))

(deftest test-range-resp-encodes-content-location
  (testing "range-resp emits a URL-encoded Content-Location"
    (let [resp (ranges/range-resp "body" "/zone/home/user/Müller.txt" 456 789 0 99)]
      (is (= "/zone/home/user/M%C3%BCller.txt"
             (get-in resp [:headers "Content-Location"]))))))
