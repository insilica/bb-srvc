(ns srvc.bb.json-schema-test
  (:require [clojure.test :refer (are deftest)]
            [srvc.bb.json-schema :as sbjs]))

(deftest test-validate
  (are [m] (false? (:valid? (sbjs/validate m)))
    {}
    {"type" "madeuptype"}
    {"data" {"i" 3}
     "type" "madeuptype"}
    {"hash" "QmQTvb2ztHnsxfdDQt423WRKL8vt588b48geXKrpkW35TV"
     "type" "madeuptype"}
    {"data" {"i" 3}
     "hash" "QmQTvb2ztHnsxfdDQt423WRKL8vt588b48geXKrpkW35TV"
     "type" "madeuptype"}))

(deftest test-validate-document
  (are [m] (false? (:valid? (sbjs/validate m)))
    {"type" "document"}
    
    ; invalid uri
    {"hash" "QmavsJszLDQdc96i3ijHnT3NTKF9pWj8zvCPKfYwrKPssc"
     "type" "document"
     "uri" "pubmed.ncbi.nlm.nih.gov/31259392/"})
  (are [m] (:valid? (sbjs/validate m))
    ; neither data nor uri
    {"hash" "QmRw9szJDJL94tHcvgAAHKCNdzgvvMybcETLpVeJutDVF5"
     "type" "document"}
    
    ; data, no uri
    {"data" {"i" 3}
     "hash" "QmQTvb2ztHnsxfdDQt423WRKL8vt588b48geXKrpkW35TV"
     "type" "document"}
    
    ; uri, no data
    {"hash" "QmXBkucp4STou5DqrpcQnct6YCf7GmGmB6kcBpdC7F2UPW"
     "type" "document"
     "uri" "https://pubmed.ncbi.nlm.nih.gov/31259392/"}
    
    ; both data and uri
    {"data" {"abstract" "Sodium lauryl sulfate (SLS)-induced contact dermatitis is a commonly used model for testing the effects of different topical formulations. According to the guidelines, the volar forearms are the preferred testing site; however, other anatomical locations have been used in previous research, particularly the upper back as the clinically used site for testing different antigens."
             "title" "Anatomical location differences in sodium lauryl sulfate‐induced irritation\n"}
     "hash" "QmSvtGdEqa2YJ8hyj2ttJRRzG4PokhoxDm9m9hJHDdECsK"
     "type" "document"
     "uri" "https://pubmed.ncbi.nlm.nih.gov/31259392/"}))

(deftest test-validate-label
  (are [m] (false? (:valid? (sbjs/validate m)))
    {"type" "label"}
    {"hash" "QmVLPNRfd3mqupBtGDmCPXXWnKuHJNbEMDo95dXERf7Wkr"
     "type" "label"}
    {"data" {}
     "hash" "QmU1t2fZNiK1xYhggX4MKA873McZ5A8Cs75ZBpN5urUBPK"
     "type" "label"}
    
    ; missing hash
    {"data" {"id" "acute_tox"
             "question" "Acute toxicity?"
             "required" true
             "type" "boolean"}
     "type" "label"}
    
    ; missing id
    {"data" {"question" "Acute toxicity?"
             "required" true
             "type" "boolean"}
     "hash" "QmXnFgxX7NCqQuwe8HeTtXqE8PpMCb4WWhZKixqEYPV56J"
     "type" "label"}
    
    ; misssing question
    {"data" {"id" "acute_tox"
             "required" true
             "type" "boolean"}
     "hash" "QmPJ2sV6XxLAKi76N9y1Jbv3XjTZ9j6rYJNFKCammmRB2u"
     "type" "label"}
    
    ; missing required
    {"data" {"id" "acute_tox"
             "question" "Acute toxicity?"
             "type" "boolean"}
     "hash" "QmQWqAQUEbbDR2B26NAEmdRXd74t2k8zQv6TaSfTD4C7kd"
     "type" "label"}
    
    ; missing type
    {"data" {"id" "acute_tox"
             "question" "Acute toxicity?"
             "required" true}
     "hash" "QmY8q7189SAzSgrAvJuuqEPr92GB27rVsK3h84FRwLrmZF"
     "type" "label"})

  (are [m] (:valid? (sbjs/validate m))
    ; boolean question
    {"data" {"id" "acute_tox"
             "question" "Acute toxicity?"
             "required" true
             "type" "boolean"}
     "hash" "QmYR83ndMKA8LDdYSeHKckfkZXAEXc6sHHLzLjKemfosQ7"
     "type" "label"}
    
    ; categorical question
    {"data" {"categories" ["heart/cardiovascular" "blood/serum" "other"]
             "question" "Which organ was the focus?"
             "id" "organ"
             "required" false
             "type" "categorical"}
     "hash" "Qmf5Ffhhm2EZ9H7FeCdZLupdwYGToSG9Cc5mPByBK66NAR"
     "type" "label"}))

(deftest test-validate-label-answer
  (are [m] (false? (:valid? (sbjs/validate m)))
    {"type" "label-answer"}
    {"hash" "QmbPF4j48y3r13HTZGdZh3q8g1nuJnrGC4LvmeToWqDAek"
     "type" "label-answer"}
    
    ; invalid reviewer URI
    {"data" {"answer" true
             "document" "Qmdaochg1sxH88Ki33NCwBku46Yi8RZogt92RQ9P5q4oB2"
             "label" "Qma9V5PYcku4pw1gmMGQoNUuZUVBeCHmzZaH9FBvomV92S"
             "reviewer" "user@example.com"
             "timestamp" 1656624308}
     "hash" "QmVmRg5RLbVYmq1BtdEhwhY8V8hHem9Giwg1YKQ8T9dTvq"
     "type" "label-answer"}
    
    ; missing hash
    {"data" {"answer" true
             "document" "Qmdaochg1sxH88Ki33NCwBku46Yi8RZogt92RQ9P5q4oB2"
             "label" "Qma9V5PYcku4pw1gmMGQoNUuZUVBeCHmzZaH9FBvomV92S"
             "reviewer" "mailto:user@example.com"
             "timestamp" 1656624308}
     "type" "label-answer"}
    
    ; missing answer
    {"data" {"document" "Qmdaochg1sxH88Ki33NCwBku46Yi8RZogt92RQ9P5q4oB2"
             "label" "Qma9V5PYcku4pw1gmMGQoNUuZUVBeCHmzZaH9FBvomV92S"
             "reviewer" "mailto:user@example.com"
             "timestamp" 1656624308}
     "hash" "QmSnj35a59zcqLWbKSKPQ1RfexnLjXtycD8aGaNpDndevP"
     "type" "label-answer"}
    
    ; missing document
    {"data" {"answer" true
             "label" "Qma9V5PYcku4pw1gmMGQoNUuZUVBeCHmzZaH9FBvomV92S"
             "reviewer" "mailto:user@example.com"
             "timestamp" 1656624308}
     "hash" "QmWQdj7TmKcdp2nwqNGEruGicUsZu7xkrQCCwi7mkadxTU"
     "type" "label-answer"}
    
    ; missing label
    {"data" {"answer" true
             "document" "Qmdaochg1sxH88Ki33NCwBku46Yi8RZogt92RQ9P5q4oB2"
             "reviewer" "mailto:user@example.com"
             "timestamp" 1656624308}
     "hash" "QmdiFrK5DSkTv1vVGYrQJUmQ7vSLKKxjkQvqUEe8HjWeUR"
     "type" "label-answer"}
    
    ; missing reviewer
    {"data" {"answer" true
             "document" "Qmdaochg1sxH88Ki33NCwBku46Yi8RZogt92RQ9P5q4oB2"
             "label" "Qma9V5PYcku4pw1gmMGQoNUuZUVBeCHmzZaH9FBvomV92S"
             "timestamp" 1656624308}
     "hash" "QmUeRmyFVkt1X1GiYgo1mGNJFRfW66qi14Bh9oUC674Z5L"
     "type" "label-answer"}
    
    ; missing timestamp
    {"data" {"answer" true
             "document" "Qmdaochg1sxH88Ki33NCwBku46Yi8RZogt92RQ9P5q4oB2"
             "label" "Qma9V5PYcku4pw1gmMGQoNUuZUVBeCHmzZaH9FBvomV92S"
             "reviewer" "mailto:user@example.com"}
     "hash" "QmZ13TovPsZSuTSkN7hCTaR9YMEzfkW9Vk64mThUkysAXj"
     "type" "label-answer"})

  (are [m] (:valid? (sbjs/validate m))
    ; boolean answer
    {"data" {"answer" true
             "document" "Qmdaochg1sxH88Ki33NCwBku46Yi8RZogt92RQ9P5q4oB2"
             "label" "Qma9V5PYcku4pw1gmMGQoNUuZUVBeCHmzZaH9FBvomV92S"
             "reviewer" "mailto:user@example.com"
             "timestamp" 1656624308}
     "hash" "QmT6CXDakK4HxgWyagMNowFLhZqRvwRZxRgGq73Pg7p5xw"
     "type" "label-answer"}
    
    ; recogito annotation
    {"data" {"answer" [{"chunk_comment" [] "chunk_end" 265 "chunk_start" 259 "chunk_text" "evoked" "id" "#fcf98d3b-ca5e-4d7a-bf40-cfae7588d58e" "label" ["ueue"] "type" "TAG"}]
             "document" "QmZRBoXcbtGsUJfxGonXhk9WMW43tGnow6C9vPSkwh3nok"
             "label" "QmfFFJqfy1XeNWv8McBF5E7aeHTNMcH7rBcJyfrLLai9WZ"
             "reviewer" "mailto:user@example.com"
             "timestamp" 1656624155}
     "hash" "QmcRYZeqKTDeXVXaipv9USRpBR9YNtsUivZi3Ngh8chz5Y"
     "type" "label-answer"}))
