import string


def permutation(q, ans, f):
    if (len(q) <= 1):
        f.write(ans + q + " ")
    else:
        for i in range(len(q)):
            permutation(q[0:i]+q[i+1:], ans+q[i], f)


path = './words.txt'

f = open(path, mode='w')

for i in range(8):
    permutation(string.ascii_letters[0:i], "", f)
    f.write('\n')
f.close()
