;;;; Python style generators: "yield" may occur in stmt lists

(require "mlib")  



;; CPS-transformiert "forms" in 
;;
;;   (lambda () (append (butlast forms) (list (last forms) continuation)))
;;
;; aber: jedes vorkommen von yield beendet das aktuelle lambda, und es wird ein neues lambda beginnend mit der jeweiligen continuation von "yield" begonnen.
#|

1, nil                                             -> (lambda () (list 1 nil))
(f), nil                                           -> (lambda () (list (f) nil))
1 2 (f1) (f2), cont                                -> (lambda () 1 2 (f1) (list (f2) cont))
(f1) (f2) (yield 123) (f3) (f4) (f5), cont         -> (lambda () (f1) (f2) (list 123 (lambda () (f3) (f4) (list (f5) cont))))
(progn (f1) (f2) (yield 123) (f3) (f4) (f5)), cont -> (lambda () (f1) (f2) (list 123 (lambda () (f3) (f4) (list (f5) cont))))

(f1)
(f2)
(if t (yield 1) (yield 2))
(f3), cont                                 -> (lambda () (f1) (f2) (if t (lambda () (list 1 (append (f3) cont))) (lambda () (list 2 (append (f3) cont)))))

|#

(defun convert-forms (forms continuation)
  (collecting (collect 'lambda)
              (collect ())
              (let loop ((forms forms))
                           (cond ((cdr forms)
                                  (let ((form (car forms)))
                                    (cond ((and (consp form) (eq 'yield (car form)))
                                           (collect `(list ,(cadr form) ,(convert-forms (cdr forms) continuation))))

                                          ((and (consp form) (eq 'progn (car form)))
                                           (loop (append (cdr form) (cdr forms))))

                                          (t
                                           (collect form)
                                           (loop (cdr forms))))))

                                 ;; last form aka "return stmt"
                                 (t
                                  (let ((form (car forms)))
                                    (cond ((and (consp form) (eq 'yield (car form)))
                                           (collect `(list ,(cadr form) ,continuation)))

                                          ((and (consp form) (eq 'progn (car form)))
                                           (loop (append (cdr form) (cdr forms))))

                                          (t
                                           (if continuation
                                                 (collect `(list ,form ,(convert-forms (cdr forms) continuation)))
                                             (collect `(list ,form nil)))))))))))



;;; verpackt `forms` in einen generator, also ein parameterloses lambda,
;;; das bei mehrfachen aufrufen aufeinanderfolgende [value valid] values liefert.
;;;
;;; "forms" darf "yield" enthalten (nur innererhalb expliziten oder impliziten progn, nicht in einem "let dynamic", nicht verschachtelt in "let dynamic")
;;; define/ defun/ defmacro sind verboten
(defmacro scan-coroutine forms
  (let ((cont (gensym "cont")) (old (gensym "old")) (val (gensym "val")) (newcont (gensym "newcont"))) 
    (cond ((null forms) '(lambda () (values nil nil)))

          (t `(let ((,cont ,(convert-forms forms nil)))
                (lambda ()
                  (if ,cont
                        (let ((,old ,cont))
                          (setq ,cont (lambda () (error "generator is a zombie")))
                          (destructuring-bind (,val ,newcont) (,old)
                            (setq ,cont ,newcont)
                            (values ,val t)))
                    (values nil nil))))))))



;; test stuff
(defmacro run (name printconvert-p printexpand-p . forms)
  `(progn (writeln)
          (writeln ,name nil)
          (write "forms:     " nil) (writeln ',forms)
          ,(when printconvert-p
             `(progn (write "converted: " nil) (writeln ',(convert-forms forms nil))))
          ,(when printexpand-p
             `(progn
                (pprint (macroexpand-1 '(scan-coroutine ,@forms)))
                (writeln)))
          (dogenerator (x (scan-coroutine ,@forms))
            (write "dogenerator: " nil)
            (writeln x))))

(define *errors* 0)
(defmacro compare (forms expected)
  `(let ((actual (convert-forms ,forms nil)))
    (unless (equal ,expected actual)
      (incf *errors*)
      (writeln "for the input forms" nil)
      (writeln ,forms)
      (writeln "Error - expected conversion:" nil)
      (writeln ,expected)
      (writeln "actual conversion:" nil)
      (writeln actual))))



;; end with yield
(run "Test 1" nil nil (writeln 1) (writeln 2) (yield (writeln 10)) (writeln 3) (writeln 4) (yield (writeln 20)))
(compare            '((writeln 1) (writeln 2) (yield (writeln 10)) (writeln 3) (writeln 4) (yield (writeln 20)))
                     '(lambda nil (writeln 1) (writeln 2) (list (writeln 10) (lambda nil (writeln 3) (writeln 4) (list (writeln 20) nil)))))


;; end with normal form
(run "Test 2" nil nil (writeln 1) (writeln 2) (yield 10) (writeln 3) (writeln 4) (yield 20) (writeln 5))
(compare            '((writeln 1) (writeln 2) (yield 10) (writeln 3) (writeln 4) (yield 20) (writeln 5))
                     '(lambda nil (writeln 1) (writeln 2) (list 10 (lambda nil (writeln 3) (writeln 4) (list 20 (lambda nil (list (writeln 5) nil)))))))


;; progn that contains yield
(run "Test 3" nil nil (writeln 1) (progn (writeln 2) (yield 10) (writeln 3)) (writeln 4) (yield 20))
(compare            '((writeln 1) (progn (writeln 2) (yield 10) (writeln 3)) (writeln 4) (yield 20))
                     '(lambda nil (writeln 1) (writeln 2) (list 10 (lambda nil (writeln 3) (writeln 4) (list 20 nil)))))


;; progn that ends with yield
(run "Test 4" nil nil (writeln 1) (progn (writeln 2) (yield (writeln 10))) (writeln 3) (writeln 4) (yield 20) (writeln 5))
(compare            '((writeln 1) (progn (writeln 2) (yield (writeln 10))) (writeln 3) (writeln 4) (yield 20) (writeln 5))
                     '(lambda nil (writeln 1) (writeln 2) (list (writeln 10) (lambda nil (writeln 3) (writeln 4) (list 20 (lambda nil (list (writeln 5) nil)))))))


;; end with progn that ends with yield
(run "Test 5" nil nil (writeln 1) (progn (writeln 2) (yield (writeln 10))) (writeln 3) (writeln 4) (yield 20) (progn (yield (writeln 5))))
(compare            '((writeln 1) (progn (writeln 2) (yield (writeln 10))) (writeln 3) (writeln 4) (yield 20) (progn (yield (writeln 5))))
                     '(lambda nil (writeln 1) (writeln 2) (list (writeln 10) (lambda nil (writeln 3) (writeln 4) (list 20 (lambda nil (list (writeln 5) nil)))))))


;; if clause whose consequent is a single yield
;(run "Test 6" t nil (writeln 1) (progn (writeln 2) (if t (yield (writeln 10)))) (writeln 3) (writeln 4) (yield 20) (writeln 5))
;(compare          '((writeln 1) (progn (writeln 2) (if t (yield (writeln 10)))) (writeln 3) (writeln 4) (yield 20) (writeln 5))
;                   '())

(unless (zerop *errors*)
  (writeln)
  (write *errors*)
  (writeln " errors" nil))

nil